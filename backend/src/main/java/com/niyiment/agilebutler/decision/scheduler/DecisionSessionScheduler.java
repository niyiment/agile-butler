package com.niyiment.agilebutler.decision.scheduler;

import com.niyiment.agilebutler.decision.model.DecisionSession;
import com.niyiment.agilebutler.decision.repository.DecisionSessionRepository;
import com.niyiment.agilebutler.decision.service.DecisionSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Periodically checks for TIMED_POLL sessions whose closesAt has passed
 * and closes them automatically.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionSessionScheduler {

    private final DecisionSessionRepository sessionRepository;
    private final DecisionSessionService sessionService;

    @Scheduled(fixedDelay = 60_000)
    public void closeExpiredSessions() {
        List<DecisionSession> expired = sessionRepository.findExpiredTimedPolls(Instant.now());

        if (expired.isEmpty()) return;

        log.info("Auto-closing {} expired timed poll(s)", expired.size());

        expired.forEach(session -> {
            try {
                sessionService.doCloseSession(session);
                log.info("Auto-closed session: '{}'", session.getTitle());
            } catch (Exception e) {
                log.error("Failed to auto-close session {}: {}", session.getId(), e.getMessage());
            }
        });
    }
}
