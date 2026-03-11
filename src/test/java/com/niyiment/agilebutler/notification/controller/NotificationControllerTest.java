package com.niyiment.agilebutler.notification.controller;

import com.niyiment.agilebutler.notification.service.NotificationService;
import com.niyiment.agilebutler.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
    }

    @Test
    void shouldGetUnreadCount() {
        when(notificationService.countUnread(userId)).thenReturn(5L);

        var response = notificationController.unreadCount(testUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(5L);
        verify(notificationService).countUnread(userId);
    }

    @Test
    void shouldMarkAsRead() {
        UUID notificationId = UUID.randomUUID();

        var response = notificationController.markRead(testUser, notificationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().message()).isEqualTo("Notification marked as read");
        verify(notificationService).markRead(notificationId, userId);
    }

    @Test
    void shouldMarkAllRead() {
        when(notificationService.markAllRead(userId)).thenReturn(3);

        var response = notificationController.markAllRead(testUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(3);
        verify(notificationService).markAllRead(userId);
    }
}
