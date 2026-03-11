package com.niyiment.agilebutler.notification.service;

import com.google.firebase.messaging.*;
import com.niyiment.agilebutler.decision.dto.request.SessionCreatedPayload;
import com.niyiment.agilebutler.decision.event.SessionClosedEvent;
import com.niyiment.agilebutler.notification.dto.response.NotificationResponse;
import com.niyiment.agilebutler.notification.model.Notification;
import com.niyiment.agilebutler.notification.model.NotificationType;
import com.niyiment.agilebutler.notification.repository.NotificationRepository;
import com.niyiment.agilebutler.standup.event.StandupSubmittedEvent;
import com.niyiment.agilebutler.user.model.Role;
import com.niyiment.agilebutler.user.model.User;
import com.niyiment.agilebutler.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;


/**
 * Orchestrates the delivery of real-time and push notifications to keep users informed of critical team events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final FirebaseMessaging firebaseMessaging;

    /**
     * Alerts managers when a team member reports a blocker, enabling rapid intervention.
     */
    @Async
    @EventListener
    @Transactional
    public void onStandupSubmitted(StandupSubmittedEvent event) {
        if (!event.hasBlocker()) return;

        userRepository.findAllByTeamIdAndActiveTrue(event.teamId())
                .stream()
                .filter(u -> u.getRole() == Role.SCRUM_MASTER
                        || u.getRole() == Role.ADMIN)
                .forEach(manager ->
                    createAndPush(manager,
                            NotificationType.BLOCKER_FLAGGED,
                            "Blocker flagged by " + event.userName(),
                            event.userName() + " has a blocker in today's standup",
                            null
                    )
                );
    }

    /**
     * Notifies all team members of the outcome once a decision session is finalized.
     */
    @Async
    @EventListener
    @Transactional
    public void onSessionClosed(SessionClosedEvent event) {
        List<User> teamMembers = userRepository.findAllByTeamIdAndActiveTrue(event.teamId());
        teamMembers.forEach(member ->
                createAndPush(member,
                        NotificationType.DECISION_SESSION_CLOSED,
                        "✅ Decision made!",
                        "Result: " + event.winningOption(),
                        event.sessionId())
        );
    }

    /**
     * Invites relevant team members to participate in a newly created decision session.
     */
    @Async
    @EventListener
    @Transactional
    public void onSessionCreatedEvent(SessionCreatedPayload payload) {
        log.info("Event: session.created received for session '{}'", payload.title());
        List<User> teamMembers = userRepository.findAllByTeamIdAndActiveTrue(payload.teamId());
        teamMembers.stream()
                .filter(u -> !u.getId().equals(payload.createdById()))
                .forEach(member -> createAndPush(
                        member,
                        NotificationType.DECISION_SESSION_CREATED,
                        "New decision session",
                        "\"" + payload.title() + "\" needs your vote",
                        payload.sessionId()
                ));
    }

    /**
     * Sends a personalized nudge to users who haven't yet submitted their daily standup.
     */
    @Transactional
    public void sendStandupReminder(User user) {
        createAndPush(user,
                NotificationType.STANDUP_REMINDER,
                "🌅 Time for your standup!",
                "Share what you did yesterday, what you're doing today, and any blockers",
                null);
    }

    /**
     * Dispatches notifications for new decision sessions to all provided members.
     */
    @Transactional
    public void notifyNewSession(List<User> members, UUID sessionId, String sessionTitle) {
        members.forEach(member ->
                createAndPush(member,
                        NotificationType.DECISION_SESSION_CREATED,
                        "🗳️ New decision session",
                        "\"" + sessionTitle + "\" needs your vote",
                        sessionId)
        );
    }

    /**
     * Retrieves a paginated history of notifications for a user's activity feed.
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getForUser(UUID userId, int page, int size) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(NotificationResponse::from);
    }

    /**
     * Counts unread notifications to provide accurate badge indicators in the UI.
     */
    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * Updates a specific notification's status to 'read' when a user interacts with it.
     */
    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        notificationRepository.findByIdAndUserId(notificationId, userId)
                .ifPresent(n -> {
                    n.setRead(true);
                    n.setReadAt(Instant.now());
                    notificationRepository.save(n);
                });
    }

    /**
     * Efficiently marks all of a user's notifications as read to clear their inbox.
     */
    @Transactional
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllReadForUser(userId, Instant.now());
    }

    /**
     * Persists a notification and broadcasts it via WebSocket and FCM for cross-platform reach.
     */
    private void createAndPush(User user, NotificationType type,
                               String title, String body,
                               UUID referenceId) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .referenceId(referenceId)
                .build();
        notification = notificationRepository.save(notification);

        simpMessagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/notifications",
                NotificationResponse.from(notification));

        if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
            sendFcmPush(user.getFcmToken(), title, body, notification.getId().toString());
            notification.setPushSent(true);
            notificationRepository.save(notification);
        }
    }

    /**
     * Delivers a push notification through Firebase Cloud Messaging for mobile devices.
     */
    private void sendFcmPush(String token, String title, String body, String notificationId) {
        try {
            var message = Message.builder()
                    .setToken(token)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("notificationId", notificationId)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder().setSound("default").build()).build())
                    .build();
            String response = firebaseMessaging.send(message);
            log.debug("FCM push sent: {}", response);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM push failed for token {}: {}", 
                token.substring(0, Math.min(token.length(), 10)) + "...", e.getMessage());
        }
    }
}
