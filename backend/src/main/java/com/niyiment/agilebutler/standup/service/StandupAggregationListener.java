package com.niyiment.agilebutler.standup.service;

import com.niyiment.agilebutler.standup.scheduler.StandupScheduler;
import com.niyiment.agilebutler.standup.repository.StandupRepository;
import com.niyiment.agilebutler.user.model.User;
import com.niyiment.agilebutler.user.repository.UserRepository;
import com.niyiment.agilebutler.notification.service.NotificationService;
import com.niyiment.agilebutler.user.model.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StandupAggregationListener {

    private final StandupRepository standupRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Async
    @EventListener
    @Transactional(readOnly = true)
    public void onStandupAggregationTrigger(StandupScheduler.StandupAggregationPayload payload) {
        log.info("Aggregating standups for team {} on {}", payload.teamId(), payload.date());
        
        List<User> allMembers = userRepository.findAllByTeamIdAndActiveTrue(payload.teamId());
        long submittedCount = standupRepository.countSubmittedByTeamAndDate(payload.teamId(), payload.date());
        
        if (submittedCount < allMembers.size()) {
            Set<UUID> submittedUserIds = standupRepository.findSubmittedByTeamAndDate(payload.teamId(), payload.date())
                    .stream()
                    .map(s -> s.getUser().getId())
                    .collect(Collectors.toSet());
            
            List<User> missing = allMembers.stream()
                    .filter(u -> !submittedUserIds.contains(u.getId()))
                    .toList();
            
            log.info("Team {}: {}/{} submitted. Missing: {}", 
                payload.teamId(), submittedCount, allMembers.size(), 
                missing.stream().map(User::getName).toList());
                
            // Notify Scrum Master about missing submissions if any
            allMembers.stream()
                .filter(u -> u.getRole() == Role.SCRUM_MASTER)
                .forEach(sm -> {
                    // This is just internal logging for now as per MVP magic
                    // In a real app we might send a summary notification
                });
        }
    }
}
