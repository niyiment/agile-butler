package com.niyiment.agilebutler.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.niyiment.agilebutler.notification.model.Notification;
import com.niyiment.agilebutler.notification.model.NotificationType;
import com.niyiment.agilebutler.notification.repository.NotificationRepository;
import com.niyiment.agilebutler.user.model.User;
import com.niyiment.agilebutler.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;
    @Mock
    private FirebaseMessaging firebaseMessaging;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setFcmToken("test-fcm-token");
    }

    @Test
    void shouldMarkAsRead() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUser(testUser);
        notification.setRead(false);

        when(notificationRepository.findByIdAndUserId(notificationId, userId))
                .thenReturn(Optional.of(notification));

        notificationService.markRead(notificationId, userId);

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    void shouldCountUnread() {
        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(5L);

        long count = notificationService.countUnread(userId);

        assertThat(count).isEqualTo(5L);
        verify(notificationRepository).countByUserIdAndReadFalse(userId);
    }

    @Test
    void shouldCreateAndPushNotification() throws Exception {
        // Since createAndPush is private, we test it through a public method like sendStandupReminder
        Notification savedNotification = Notification.builder()
                .user(testUser)
                .type(NotificationType.STANDUP_REMINDER)
                .title("Title")
                .body("Body")
                .build();
        savedNotification.setId(UUID.randomUUID());

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(firebaseMessaging.send(any(Message.class))).thenReturn("fcm-response");

        notificationService.sendStandupReminder(testUser);

        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
        verify(simpMessagingTemplate).convertAndSendToUser(eq(testUser.getEmail()), anyString(), any());
        verify(firebaseMessaging).send(any(Message.class));
    }
}
