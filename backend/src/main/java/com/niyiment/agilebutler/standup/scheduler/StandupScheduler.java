package com.niyiment.agilebutler.standup.scheduler;


import com.niyiment.agilebutler.notification.service.NotificationService;
import com.niyiment.agilebutler.team.model.Team;
import com.niyiment.agilebutler.team.repository.TeamRepository;
import com.niyiment.agilebutler.user.model.User;
import com.niyiment.agilebutler.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled jobs for the Standup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StandupScheduler {
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 * * * * *")
    public void sendStandupReminders() {
        List<User> candidates = userRepository.findAllActiveUsersWithNotificationTime();

        List<User> dueUsers = candidates.stream()
                .filter(user -> {
                    ZoneId zoneId = safeZone(user.getTimezone());
                    LocalTime now = ZonedDateTime.now(zoneId)
                            .toLocalTime()
                            .withSecond(0).withNano(0);
                    LocalTime set = user.getNotificationTime()
                            .withSecond(0).withNano(0);

                    return set.equals(now);
                }).toList();

        if (dueUsers.isEmpty()) return;

        log.info("Sending standup reminders to {} user(s)", dueUsers.size());
        dueUsers.forEach(user -> {
            try {
                notificationService.sendStandupReminder(user);
            } catch (Exception e) {
                log.error("Failed to send standup reminder to {} : {}", user.getEmail(), e.getMessage());
            }
        });
    }

    @Scheduled(cron = "0 * * * * *")
    public void triggerDeadlineAggregation() {
        List<Team> deadlineTeams = teamRepository.findAllActive()
                .stream()
                .filter(team -> {
                    ZoneId zone = safeZone(team.getTimezone());
                    LocalTime now = ZonedDateTime.now(zone)
                            .toLocalTime()
                            .withSecond(0).withNano(0);
                    return team.getStandupDeadlineTime()
                            .withSecond(0).withNano(0)
                            .equals(now);
                })
                .toList();

        if (deadlineTeams.isEmpty()) return;

        log.info("Triggering standup aggregation for {} team(s)", deadlineTeams.size());
        deadlineTeams.forEach(team -> {
            try {
                // Use the team's local date — avoids midnight-boundary bugs.
                LocalDate teamToday = ZonedDateTime.now(safeZone(team.getTimezone())).toLocalDate();
                var payload = new StandupAggregationPayload(team.getId(), teamToday);
                eventPublisher.publishEvent(payload);
                log.info("Aggregation event published for team '{}' on {}", team.getName(), teamToday);
            } catch (Exception e) {
                log.error("Aggregation publish failed for team {}: {}", team.getName(), e.getMessage());
            }
        });
    }

    private ZoneId safeZone(String timezone) {
        try {
            return (timezone != null && !timezone.isBlank())
                    ? ZoneId.of(timezone) : ZoneId.of("UTC");
        } catch (Exception e) {
            log.warn("Invalid timezone: {}, falling back to UTC", timezone);
            return ZoneId.of("UTC");
        }
    }

    public record StandupAggregationPayload(
            UUID teamId, LocalDate date
    ) {

    }
}

