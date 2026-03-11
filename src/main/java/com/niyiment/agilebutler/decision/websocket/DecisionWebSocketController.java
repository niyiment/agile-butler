package com.niyiment.agilebutler.decision.websocket;


import com.niyiment.agilebutler.decision.dto.request.AddCommentRequest;
import com.niyiment.agilebutler.decision.dto.request.CastMultiVoteRequest;
import com.niyiment.agilebutler.decision.dto.request.CastVoteRequest;
import com.niyiment.agilebutler.decision.dto.response.PresenceMessage;
import com.niyiment.agilebutler.decision.dto.response.SessionResponse;
import com.niyiment.agilebutler.decision.service.DecisionSessionService;
import com.niyiment.agilebutler.decision.service.VotingService;
import com.niyiment.agilebutler.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket STOMP controller
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DecisionWebSocketController {

    private final DecisionSessionService sessionService;
    private final VotingService votingService;
    private final SimpMessagingTemplate messaging;

    private final Map<UUID, AtomicInteger> sessionParticipants = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> wsSessionToDecisionSessions = new ConcurrentHashMap<>();
    private final Map<String, User> wsSessionToUser = new ConcurrentHashMap<>();

    @MessageMapping("/sessions/{sessionId}/vote")
    public void vote(
            @DestinationVariable UUID sessionId,
            @Payload CastVoteRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        User user = getUser(headerAccessor);
        votingService.castVote(sessionId, user.getId(), request);
    }

    @MessageMapping("/sessions/{sessionId}/multi-vote")
    public void multiVote(
            @DestinationVariable UUID sessionId,
            @Payload CastMultiVoteRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        User user = getUser(headerAccessor);
        votingService.castMultiVote(sessionId, user.getId(), request);
    }

    @MessageMapping("/sessions/{sessionId}/comment")
    public void comment(
            @DestinationVariable UUID sessionId,
            @Payload AddCommentRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        User user = getUser(headerAccessor);
        votingService.addComment(sessionId, user.getId(), request);
    }

    @MessageMapping("/sessions/{sessionId}/join")
    public void join(
            @DestinationVariable UUID sessionId,
            SimpMessageHeaderAccessor headerAccessor) {

        User user = getUser(headerAccessor);
        String wsId = headerAccessor.getSessionId();

        wsSessionToDecisionSessions
                .computeIfAbsent(wsId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
        wsSessionToUser.put(wsId, user);

        int count = sessionParticipants
                .computeIfAbsent(sessionId, k -> new AtomicInteger(0))
                .incrementAndGet();

        boolean anon = isAnonymousSession(sessionId);

        var msg = new PresenceMessage(
                sessionId, user.getId(), user.getName(),
                user.getAvatarUrl(), true, count
        );

        messaging.convertAndSend("/topic/sessions/" + sessionId + "/presence", msg);
        log.debug("User {} joined session {} ({} participants)", user.getName(), sessionId, count);
    }

    @SubscribeMapping("/sessions/{sessionId}/votes")
    public SessionResponse onSubscribeVotes(
            @DestinationVariable UUID sessionId) {
        return sessionService.getSession(sessionId);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String wsId = event.getSessionId();

        Set<UUID> joinedSessions = wsSessionToDecisionSessions.remove(wsId);
        User user = wsSessionToUser.remove(wsId);
        if (joinedSessions == null || joinedSessions.isEmpty()) return;
        joinedSessions.forEach(sessionId -> {
            AtomicInteger counter = sessionParticipants.get(sessionId);
            if (counter == null) return;

            int count = Math.max(0, counter.decrementAndGet());
            boolean anon = isAnonymousSession(sessionId);

            var msg = new PresenceMessage(
                    sessionId,
                    anon || user == null ? null : user.getId(),
                    anon || user == null ? "Anonymous" : user.getName(),
                    anon || user == null ? null : user.getAvatarUrl(),
                    false,
                    count
            );
            messaging.convertAndSend("/topic/sessions/" + sessionId + "/presence", msg);
            log.debug("User disconnected from session {} ({} remainig)", sessionId, count);
        });
    }

    private User getUser(SimpMessageHeaderAccessor accessor) {
        if (accessor.getUser() instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof User user) {
            return user;
        }
        throw new IllegalStateException("WebSocket session is not authenticated");
    }

    private boolean isAnonymousSession(UUID sessionId) {
        try {
            return sessionService.getSession(sessionId).anonymous();
        } catch (Exception e) {
            return false;
        }
    }
}
