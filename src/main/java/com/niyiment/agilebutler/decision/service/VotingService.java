package com.niyiment.agilebutler.decision.service;

import com.niyiment.agilebutler.common.exception.BusinessException;
import com.niyiment.agilebutler.common.exception.ResourceNotFoundException;
import com.niyiment.agilebutler.decision.dto.request.*;
import com.niyiment.agilebutler.decision.dto.response.CommentMessage;
import com.niyiment.agilebutler.decision.dto.response.CommentResponse;
import com.niyiment.agilebutler.decision.dto.response.OptionResponse;
import com.niyiment.agilebutler.decision.dto.response.VoteUpdateMessage;
import com.niyiment.agilebutler.decision.model.Comment;
import com.niyiment.agilebutler.decision.model.DecisionOption;
import com.niyiment.agilebutler.decision.model.DecisionSession;
import com.niyiment.agilebutler.decision.model.Vote;
import com.niyiment.agilebutler.decision.model.enums.DecisionType;
import com.niyiment.agilebutler.decision.repository.CommentRepository;
import com.niyiment.agilebutler.decision.repository.DecisionOptionRepository;
import com.niyiment.agilebutler.decision.repository.DecisionSessionRepository;
import com.niyiment.agilebutler.decision.repository.VoteRepository;
import com.niyiment.agilebutler.user.model.User;
import com.niyiment.agilebutler.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Manages the voting lifecycle, including casting votes across different session types and handling participant comments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VotingService {
    private final DecisionSessionRepository decisionSessionRepository;
    private final UserService userService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private final VoteRepository voteRepository;
    private final DecisionOptionRepository decisionOptionRepository;
    private final CommentRepository commentRepository;

    /**
     * Records a single-choice vote and broadcasts the updated standings to the team.
     */
    @Transactional
    public void castVote(UUID sessionId, UUID userId, CastVoteRequest request) {
        DecisionSession session = findOpenSessionForUpdate(sessionId);
        User user = userService.findOrThrow(userId);

        if (session.getDecisionType() == DecisionType.MULTIPLE_CHOICE) {
            throw new BusinessException("Use the multi-vote endpoint for MULTIPLE_CHOICE sessions");
        }
        if (session.getDecisionType() == DecisionType.RANKED) {
            throw new BusinessException("Use the ranked-vote endpoint for RANKED sessions");
        }

        DecisionOption option = decisionOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new ResourceNotFoundException("Option", request.optionId()));

        validateOptionBelongsToSession(option, sessionId);

        // PESSIMISTIC_WRITE lock — serialises concurrent vote changes for this user.
        voteRepository.deleteAllBySessionIdAndUserId(sessionId, userId);

        Vote vote = Vote.builder()
                .session(session)
                .user(user)
                .option(option)
                .commentText(request.commentText())
                .build();

        voteRepository.save(vote);
        log.debug("Vote cast by {} in session {}", user.getEmail(), sessionId);

        eventPublisher.publishEvent(new VoteCastPayload(sessionId, userId, option.getId()));

        pushVoteUpdate(session);
    }

    // ── Multiple-choice vote ──────────────────────────────────────────────────

    /**
     * Records multiple selections for a session, replacing any previous choices by the same user.
     */
    @Transactional
    public void castMultiVote(UUID sessionId, UUID userId, CastMultiVoteRequest request) {
        DecisionSession session = findOpenSessionForUpdate(sessionId);

        if (session.getDecisionType() != DecisionType.MULTIPLE_CHOICE) {
            throw new BusinessException("Session is not a MULTIPLE_CHOICE session");
        }

        User user = userService.findOrThrow(userId);

        // PESSIMISTIC_WRITE lock — atomic replace of all selections.
        voteRepository.deleteAllBySessionIdAndUserId(sessionId, userId);

        List<Vote> votes = request.optionIds().stream().map(optId -> {
            DecisionOption opt = decisionOptionRepository.findById(optId)
                    .orElseThrow(() -> new ResourceNotFoundException("Option", optId));
            validateOptionBelongsToSession(opt, sessionId);
            return Vote.builder()
                    .session(session).user(user).option(opt)
                    .commentText(request.commentText())
                    .build();
        }).toList();

        voteRepository.saveAll(votes);
        pushVoteUpdate(session);
    }

    // ── Ranked-choice vote ────────────────────────────────────────────────────

    /**
     * Accepts an ordered list of option IDs from first to last preference.
     * The index position determines the rank (0 = most preferred).
     */
    /**
     * Processes a ranked-choice ballot, applying Borda-style weights to reflect user preferences.
     */
    @Transactional
    public void castRankedVote(UUID sessionId, UUID userId, CastRankedVoteRequest request) {
        DecisionSession session = findOpenSessionForUpdate(sessionId);

        if (session.getDecisionType() != DecisionType.RANKED) {
            throw new BusinessException("Session is not a RANKED session");
        }

        int numOptions = decisionSessionRepository.findAllBySessionIdOrderByDisplayOrderAsc(sessionId).size();
        if (request.rankedOptionIds().size() != numOptions) {
            throw new BusinessException(
                    "Ranked vote must include all %d options".formatted(numOptions));
        }

        User user = userService.findOrThrow(userId);

        // PESSIMISTIC_WRITE lock — atomic replace.
        voteRepository.deleteAllBySessionIdAndUserId(sessionId, userId);

        List<Vote> votes = new ArrayList<>();
        for (int rank = 0; rank < request.rankedOptionIds().size(); rank++) {
            UUID optionId = request.rankedOptionIds().get(rank);
            DecisionOption opt = decisionOptionRepository.findById(optionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Option", optionId));
            validateOptionBelongsToSession(opt, sessionId);
            votes.add(Vote.builder()
                    .session(session).user(user).option(opt)
                    .rankOrder(rank)
                    .build());
        }

        voteRepository.saveAll(votes);
        pushVoteUpdate(session);
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    /**
     * Adds a discussion point or reply to a session, respecting anonymity settings.
     */
    @Transactional
    public CommentResponse addComment(UUID sessionId, UUID userId,
                                      AddCommentRequest request) {
        DecisionSession session = findOpenSessionById(sessionId);
        User user = userService.findOrThrow(userId);

        Comment comment = Comment.builder()
                .session(session)
                .user(user)
                .commentText(request.commentText())
                .parentCommentId(request.parentCommentId())
                .build();

        comment = commentRepository.save(comment);

        // Apply anonymous masking before broadcasting.
        var response = buildCommentResponse(comment, session.isAnonymous());

        simpMessagingTemplate.convertAndSend(
                "/topic/sessions/" + sessionId + "/comments",
                new CommentMessage(sessionId, response));

        return response;
    }

    /**
     * Retrieves all comments for a session, masking identities if the session is anonymous.
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID sessionId) {
        boolean anonymous = decisionSessionRepository.findById(sessionId)
                .map(DecisionSession::isAnonymous)
                .orElse(false);

        return commentRepository.findAllBySessionIdWithUser(sessionId)
                .stream()
                .map(c -> buildCommentResponse(c, anonymous))
                .toList();
    }

    // ── Comment reactions ─────────────────────────────────────────────────────

    /**
     * Toggles a user's reaction on a comment to provide lightweight feedback without cluttering discussion.
     */
    @Transactional
    public CommentResponse reactToComment(UUID sessionId, UUID commentId,
                                          UUID userId,
                                          ReactToCommentRequest request) {
        // Ensure the session exists (and the user is a valid participant).
        DecisionSession session = decisionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("DecisionSession", sessionId));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        if (!comment.getSession().getId().equals(sessionId)) {
            throw new BusinessException("Comment does not belong to this session");
        }

        // Idempotent toggle: if the user has already reacted with this emoji,
        // remove it (un-react); otherwise add it.
        boolean alreadyReacted = comment.getUserReactions()
                .getOrDefault(userId.toString(), "").equals(request.emoji());

        if (alreadyReacted) {
            comment.getUserReactions().remove(userId.toString());
            comment.setReactionCount(Math.max(0, comment.getReactionCount() - 1));
        } else {
            // If the user had a DIFFERENT reaction, replace it without double-counting.
            if (comment.getUserReactions().containsKey(userId.toString())) {
                comment.getUserReactions().put(userId.toString(), request.emoji());
                // reaction count stays the same — it's a change, not a new one
            } else {
                comment.getUserReactions().put(userId.toString(), request.emoji());
                comment.setReactionCount(comment.getReactionCount() + 1);
            }
        }

        comment = commentRepository.save(comment);
        var response = buildCommentResponse(comment, session.isAnonymous());

        // Broadcast updated comment so all clients re-render reaction counts.
        simpMessagingTemplate.convertAndSend(
                "/topic/sessions/" + sessionId + "/comments",
                new CommentMessage(sessionId, response));

        return response;
    }

    // ── Option response building ───────────────────────────────────────────────

    /**
     * Dispatches to the appropriate tallying logic based on the session's decision type.
     */
    List<OptionResponse> buildOptionResponses(DecisionSession session) {
        if (session.getDecisionType() == DecisionType.RANKED) {
            return buildRankedOptionResponses(session);
        }
        return buildStandardOptionResponses(session);
    }

    /**
     * Performs a standard tally where each vote contributes equally to an option's total.
     */
    private List<OptionResponse> buildStandardOptionResponses(DecisionSession session) {
        List<DecisionOption> options =
                decisionSessionRepository.findAllBySessionIdOrderByDisplayOrderAsc(session.getId());

        Map<UUID, Long> voteCounts = voteRepository.countVotesByOption(session.getId())
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        long total = voteCounts.values().stream().mapToLong(Long::longValue).sum();

        return options.stream().map(opt -> {
            long count = voteCounts.getOrDefault(opt.getId(), 0L);
            double pct = total == 0 ? 0.0 : Math.round((double) count / total * 1000.0) / 10.0;
            return new OptionResponse(
                    opt.getId(), opt.getOptionText(), opt.getDisplayOrder(), count, pct);
        }).toList();
    }

    /**
     * Applies Borda count scoring to determine standings in a ranked-choice session.
     */
    private List<OptionResponse> buildRankedOptionResponses(DecisionSession session) {
        List<DecisionOption> options =
                decisionSessionRepository.findAllBySessionIdOrderByDisplayOrderAsc(session.getId());
        int n = options.size();
        if (n == 0) return List.of();

        // Maximum Borda score per voter = sum(0..n-1) = n*(n-1)/2
        long voterCount = voteRepository.countBySessionId(session.getId()) / n;
        long maxScore = voterCount * ((long) n * (n - 1) / 2);

        List<Vote> allVotes = voteRepository.findAllBySessionIdWithOptionForRanked(session.getId());

        // Accumulate Borda scores per option.
        Map<UUID, Long> scores = new HashMap<>();
        for (Vote v : allVotes) {
            int rank = v.getRankOrder() != null ? v.getRankOrder() : (n - 1);
            long pts = n - 1 - rank;   // rank 0 → n-1 points, rank n-1 → 0 points
            scores.merge(v.getOption().getId(), pts, Long::sum);
        }

        return options.stream().map(opt -> {
            long score = scores.getOrDefault(opt.getId(), 0L);
            double pct = maxScore == 0 ? 0.0
                    : Math.round((double) score / maxScore * 1000.0) / 10.0;
            return new OptionResponse(
                    opt.getId(), opt.getOptionText(), opt.getDisplayOrder(), score, pct);
        }).toList();
    }

    /**
     * Returns the total number of individual votes cast in a session.
     */
    long countVotes(UUID sessionId) {
        return voteRepository.countBySessionId(sessionId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Acquires a database lock on an open session to ensure atomic vote processing.
     */
    DecisionSession findOpenSessionForUpdate(UUID sessionId) {
        DecisionSession session = decisionSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("DecisionSession", sessionId));
        if (!session.isOpen()) {
            throw new BusinessException("Session is already closed");
        }
        return session;
    }

    /**
     * Locates a session and confirms it is still open for engagement.
     */
    DecisionSession findOpenSessionById(UUID sessionId) {
        DecisionSession session = decisionSessionRepository.findByIdWithDetails(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("DecisionSession", sessionId));
        if (!session.isOpen()) {
            throw new BusinessException("Session is already closed");
        }
        return session;
    }

    /**
     * Verifies that a selected option belongs to the correct session context.
     */
    private void validateOptionBelongsToSession(DecisionOption option, UUID sessionId) {
        if (!option.getSession().getId().equals(sessionId)) {
            throw new BusinessException("Option does not belong to this session");
        }
    }

    /**
     * Builds a CommentResponse, masking identity fields when the session is anonymous.
     */
    private CommentResponse buildCommentResponse(Comment c, boolean anonymous) {
        return new CommentResponse(
                c.getId(),
                anonymous ? null : c.getUser().getId(),
                anonymous ? "Anonymous" : c.getUser().getName(),
                anonymous ? null : c.getUser().getAvatarUrl(),
                c.getCommentText(),
                c.getParentCommentId(),
                c.getReactionCount(),
                c.getUserReactions(),
                c.getCreatedAt()
        );
    }

    /**
     * Broadcasts real-time vote totals and participation counts to all connected team members.
     */
    private void pushVoteUpdate(DecisionSession session) {
        var options = buildOptionResponses(session);
        long total = options.stream().mapToLong(OptionResponse::voteCount).sum();
        int participants = decisionSessionRepository.countParticipants(session.getId());

        var msg = new VoteUpdateMessage(
                session.getId(), options, (int) total, participants, Instant.now());

        simpMessagingTemplate.convertAndSend(
                "/topic/sessions/" + session.getId() + "/votes", msg);
    }

    record VoteCastPayload(UUID sessionId, UUID userId, UUID optionId) {
    }
}
