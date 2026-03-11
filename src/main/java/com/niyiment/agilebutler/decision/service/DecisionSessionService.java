package com.niyiment.agilebutler.decision.service;


import com.niyiment.agilebutler.common.exception.AccessDeniedException;
import com.niyiment.agilebutler.common.exception.BusinessException;
import com.niyiment.agilebutler.common.exception.ResourceNotFoundException;
import com.niyiment.agilebutler.decision.dto.request.CreateFromBlockerRequest;
import com.niyiment.agilebutler.decision.dto.request.CreateSessionRequest;
import com.niyiment.agilebutler.decision.dto.request.SessionClosedPayload;
import com.niyiment.agilebutler.decision.dto.request.SessionCreatedPayload;
import com.niyiment.agilebutler.decision.dto.response.OptionResponse;
import com.niyiment.agilebutler.decision.dto.response.SessionClosedMessage;
import com.niyiment.agilebutler.decision.dto.response.SessionResponse;
import com.niyiment.agilebutler.decision.dto.response.SessionSummary;
import com.niyiment.agilebutler.decision.event.SessionClosedEvent;
import com.niyiment.agilebutler.decision.model.DecisionOption;
import com.niyiment.agilebutler.decision.model.DecisionSession;
import com.niyiment.agilebutler.decision.model.enums.DecisionType;
import com.niyiment.agilebutler.decision.model.enums.SessionStatus;
import com.niyiment.agilebutler.decision.model.enums.SessionType;
import com.niyiment.agilebutler.decision.repository.DecisionSessionRepository;
import com.niyiment.agilebutler.standup.model.Standup;
import com.niyiment.agilebutler.standup.service.StandupService;
import com.niyiment.agilebutler.user.model.User;
import com.niyiment.agilebutler.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.UUID;


/**
 * Manages decision-making sessions, allowing teams to vote on proposals or resolve blockers collaboratively.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DecisionSessionService {
    private final DecisionSessionRepository sessionRepository;
    private final VotingService votingService;
    private final UserService userService;
    private final StandupService standupService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.decision.live-session-max-minutes}")
    private int liveMaxMinutes;

    @Value("${app.decision.timed-poll-max-hours}")
    private long timedPollMaxHours;

    /**
     * Initializes a new decision session with specified options and voting rules.
     */
    @Transactional
    public SessionResponse createSession(UUID creatorId,
                                         CreateSessionRequest request) {
        User creator = userService.findOrThrow(creatorId);
        if (creator.getTeam() == null) {
            throw new BusinessException("Creator must belong to a team");
        }

        // Determine closing time
        Instant closesAt = computeClosesAt(request);

        DecisionSession session = DecisionSession.builder()
                .title(request.title())
                .description(request.description())
                .team(creator.getTeam())
                .createdByUser(creator)
                .sessionType(request.sessionType())
                .decisionType(request.decisionType())
                .anonymous(request.anonymous())
                .maxDurationMinutes(request.maxDurationMinutes())
                .closesAt(closesAt)
                .category(request.category())
                .build();

        // Attach options
        List<DecisionOption> options = buildOptions(session, request.options());
        session.setOptions(options);

        session = sessionRepository.save(session);

        log.info("Decision session '{}' created by {}", session.getTitle(), creator.getEmail());

        eventPublisher.publishEvent(new SessionCreatedPayload(session.getId(), session.getTitle(),
                creator.getTeam().getId(), creatorId));

        return buildSessionResponse(session);
    }

    /**
     * Converts a reported blocker into a formal decision session to facilitate team resolution.
     */
    @Transactional
    public SessionResponse createFromBlocker(UUID creatorId, CreateFromBlockerRequest request) {
        User creator = userService.findOrThrow(creatorId);
        if (creator.getTeam() == null) {
            throw new BusinessException("Creator must belong to a team");
        }

        // Validate the standup exists and belongs to the creator's team
        Standup standup = standupService.findById(request.standupId());
        if (standup.getTeam() == null || !standup.getTeam().getId().equals(creator.getTeam().getId())) {
            throw new AccessDeniedException("Standup does not belong to your team");
        }

        DecisionSession session = DecisionSession.builder()
                .title("Blocker: " + request.blockerText())
                .description(request.blockerContext())
                .team(creator.getTeam())
                .createdByUser(creator)
                .sessionType(SessionType.LIVE)
                .decisionType(DecisionType.SINGLE_CHOICE)
                .status(SessionStatus.ACTIVE)
                .anonymous(false)
                .maxDurationMinutes(30)
                .closesAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .category("process")
                .linkedStandupId(request.standupId())
                .build();

        // Default options for blocker sessions
        List<DecisionOption> options = new ArrayList<>();
        options.add(DecisionOption.builder().session(session).optionText("Resolution A").displayOrder(0).build());
        options.add(DecisionOption.builder().session(session).optionText("Resolution B").displayOrder(1).build());
        options.add(DecisionOption.builder().session(session).optionText("Escalate").displayOrder(2).build());
        session.setOptions(options);

        session = sessionRepository.save(session);

        log.info("Blocker-based session '{}' created by {}", session.getTitle(), creator.getEmail());

        eventPublisher.publishEvent(new SessionCreatedPayload(session.getId(), session.getTitle(),
                creator.getTeam().getId(), creatorId));

        return buildSessionResponse(session);
    }

    /**
     * Explicitly terminates an active session, preventing further voting and finalizing results.
     */
    @Transactional
    public SessionResponse closeSession(UUID sessionId, UUID requesterId) {
        DecisionSession session = findOpenSession(sessionId);

        if (!session.getCreatedByUser().getId().equals(requesterId)) {
            throw new AccessDeniedException("Only the session creator can close it");
        }

        return doCloseSession(session);
    }

    /**
     * Retrieves full details and current standings for a specific decision session.
     */
    @Transactional
    public SessionResponse getSession(UUID sessionId) {
        DecisionSession session = sessionRepository.findByIdWithDetails(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("DecisionSession", sessionId));
        return buildSessionResponse(session);
    }

    /**
     * Lists all currently open decision sessions for a specific team.
     */
    @Transactional(readOnly = true)
    public List<SessionSummary> getActiveSessions(UUID teamId) {
        List<DecisionSession> sessions = sessionRepository.findByTeamIdWithDetails(teamId);
        List<Object[]> voteCounts = sessionRepository.findVoteCountsByTeamId(teamId);
        
        // Map session_id -> option_id -> count
        Map<UUID, Map<UUID, Long>> sessionVoteMap = new HashMap<>();
        for (Object[] row : voteCounts) {
            UUID sid = (UUID) row[0];
            UUID oid = (UUID) row[1];
            Long count = (Long) row[2];
            sessionVoteMap.computeIfAbsent(sid, k -> new HashMap<>()).put(oid, count);
        }

        return sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.ACTIVE)
                .map(s -> toSummaryOptimized(s, sessionVoteMap.getOrDefault(s.getId(), Map.of())))
                .toList();
    }

    /**
     * Provides a paginated history of all past and present decision sessions for a team.
     */
    @Transactional(readOnly = true)
    public Page<SessionSummary> getSessionHistory(UUID teamId, int page, int size) {
        Page<DecisionSession> sessionsPage = sessionRepository.findByTeamIdOrderByCreatedAtDesc(teamId,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()));
        
        List<UUID> sessionIds = sessionsPage.getContent().stream().map(DecisionSession::getId).toList();
        // Since we need vote counts for summary, we can either fetch them or accept N+1 on these limited items.
        // For history summary, a dedicated query for vote counts of these specific IDs is better.
        
        return sessionsPage.map(this::toSummary);
    }

    /**
     * Performs the internal state transition to close a session and broadcasts the final outcome.
     */
    @Transactional
    public SessionResponse doCloseSession(DecisionSession session) {
        session.setStatus(SessionStatus.CLOSED);
        session.setClosedAt(Instant.now());
        session = sessionRepository.save(session);

        SessionResponse response = buildSessionResponse(session);

        // Determine winning option
        String winner = response.options().stream()
                .max(java.util.Comparator.comparingLong(OptionResponse::voteCount))
                .map(OptionResponse::optionText)
                .orElse("No votes cast");

        // Push WebSocket closed event
        var msg = new SessionClosedMessage(
                session.getId(), winner, response.options(),
                response.totalVotes(), session.getClosedAt());
        messagingTemplate.convertAndSend("/topic/sessions/" + session.getId() + "/closed", msg);

        // Domain event
        eventPublisher.publishEvent(new SessionClosedEvent(session.getId(), winner, session.getTeam().getId()));

        log.info("Session '{}' closed. Winner: {}", session.getTitle(), winner);
        return response;
    }

    /**
     * Maps a session entity to a response DTO, including calculated vote totals and participation metrics.
     */
    SessionResponse buildSessionResponse(DecisionSession session) {
        List<OptionResponse> optionResponses =
                votingService.buildOptionResponses(session);

        long totalVotes = optionResponses.stream()
                .mapToLong(OptionResponse::voteCount)
                .sum();

        int participants = sessionRepository.countParticipants(session.getId());

        return new SessionResponse(
                session.getId(),
                session.getTitle(),
                session.getDescription(),
                session.getTeam().getId(),
                session.getCreatedByUser().getId(),
                session.getCreatedByUser().getName(),
                session.getSessionType(),
                session.getDecisionType(),
                session.getStatus(),
                session.isAnonymous(),
                session.getCategory(),
                session.getClosesAt(),
                session.getClosedAt(),
                optionResponses,
                (int) totalVotes,
                participants,
                session.getCreatedAt()
        );
    }

    /**
     * Locates a session and verifies it is still accepting votes before allowing further actions.
     */
    DecisionSession findOpenSession(UUID sessionId) {
        DecisionSession session = sessionRepository.findByIdWithDetails(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("DecisionSession", sessionId));
        if (!session.isOpen()) {
            throw new BusinessException("Session is already closed");
        }
        return session;
    }

    /**
     * Transforms a list of strings into structured decision options associated with a session.
     */
    private List<DecisionOption> buildOptions(DecisionSession session, List<String> texts) {
        var options = new ArrayList<DecisionOption>();
        for (int i = 0; i < texts.size(); i++) {
            var opt = new DecisionOption();
            opt.setSession(session);
            opt.setOptionText(texts.get(i));
            opt.setDisplayOrder(i);
            options.add(opt);
        }
        return options;
    }

    /**
     * Calculates the session's expiration time based on its type and duration settings.
     */
    private Instant computeClosesAt(CreateSessionRequest request) {
        if (request.sessionType() == SessionType.LIVE) {
            int minutes = request.maxDurationMinutes() != null
                    ? Math.min(request.maxDurationMinutes(), liveMaxMinutes)
                    : liveMaxMinutes;
            return Instant.now().plus(minutes, ChronoUnit.MINUTES);
        } else {
            return Instant.now().plus(timedPollMaxHours, ChronoUnit.HOURS);
        }
    }

    /**
     * Creates a lightweight summary of a session for list-based displays.
     */
    private SessionSummary toSummary(DecisionSession session) {
        String winner = null;
        if (session.getStatus() == SessionStatus.CLOSED) {
            winner = votingService.buildOptionResponses(session).stream()
                    .max(java.util.Comparator.comparingLong(OptionResponse::voteCount))
                    .map(OptionResponse::optionText)
                    .orElse(null);
        }
        return new SessionSummary(
                session.getId(), session.getTitle(),
                session.getSessionType(), session.getStatus(),
                winner,
                (int) votingService.countVotes(session.getId()),
                session.getClosedAt(), session.getCategory(), session.getCreatedAt()
        );
    }

    private SessionSummary toSummaryOptimized(DecisionSession session, Map<UUID, Long> optionVotes) {
        String winner = null;
        long totalVotes = optionVotes.values().stream().mapToLong(L -> L).sum();
        
        if (session.getStatus() == SessionStatus.CLOSED) {
            winner = session.getOptions().stream()
                    .max(java.util.Comparator.comparingLong(o -> optionVotes.getOrDefault(o.getId(), 0L)))
                    .map(DecisionOption::getOptionText)
                    .orElse(null);
        }
        
        return new SessionSummary(
                session.getId(), session.getTitle(),
                session.getSessionType(), session.getStatus(),
                winner,
                (int) totalVotes,
                session.getClosedAt(), session.getCategory(), session.getCreatedAt()
        );
    }


}
