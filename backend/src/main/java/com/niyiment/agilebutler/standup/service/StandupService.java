package com.niyiment.agilebutler.standup.service;

import com.niyiment.agilebutler.common.exception.BusinessException;
import com.niyiment.agilebutler.common.exception.ResourceNotFoundException;
import com.niyiment.agilebutler.standup.dto.request.AddAttachmentRequest;
import com.niyiment.agilebutler.standup.dto.request.SaveDraftRequest;
import com.niyiment.agilebutler.standup.dto.request.StandupStatusUpdate;
import com.niyiment.agilebutler.standup.dto.request.SubmitStandupRequest;
import com.niyiment.agilebutler.standup.dto.response.AttachmentResponse;
import com.niyiment.agilebutler.standup.dto.response.StandupResponse;
import com.niyiment.agilebutler.standup.dto.response.TeamStandupSummary;
import com.niyiment.agilebutler.standup.event.StandupSubmittedEvent;
import com.niyiment.agilebutler.standup.model.Standup;
import com.niyiment.agilebutler.standup.model.StandupAttachment;
import com.niyiment.agilebutler.standup.model.enums.AttachmentType;
import com.niyiment.agilebutler.standup.model.enums.StandupStatus;
import com.niyiment.agilebutler.standup.repository.StandupAttachmentRepository;
import com.niyiment.agilebutler.standup.repository.StandupRepository;
import com.niyiment.agilebutler.team.model.Team;
import com.niyiment.agilebutler.team.repository.TeamRepository;
import com.niyiment.agilebutler.user.model.User;
import com.niyiment.agilebutler.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Manages daily standup submissions, drafts, and summaries to track team progress and blockers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StandupService {
    private final StandupRepository standupRepository;
    private final StandupAttachmentRepository standupAttachmentRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate simpMessagingTemplate;

    /**
     * Records a completed standup submission and triggers notifications if blockers are present.
     */
    @Transactional
    public StandupResponse submitStandup(UUID userId,
                                         SubmitStandupRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getTeam() == null) {
            throw new BusinessException("User must belong to a team before submitting a standup");
        }

        LocalDate today = LocalDate.now();

        Standup standup = standupRepository
                .findByUserIdAndStandupDate(userId, today)
                .orElseGet(() -> Standup.builder()
                        .user(user)
                        .team(user.getTeam())
                        .standupDate(today)
                        .build());
        standup.setYesterdayText(request.yesterdayText());
        standup.setTodayText(request.todayText());
        standup.setBlockersText(request.blockersText());
        standup.setBlockerFlagged(standup.hasBlocker());
        standup.setStatus(StandupStatus.SUBMITTED);
        standup = standupRepository.save(standup);
        log.info("Standup submitted by {} for {}", user.getEmail(), today);

        eventPublisher.publishEvent(StandupSubmittedEvent.from(standup));
        pushStatusUpdate(standup);

        return StandupResponse.from(standup);
    }

    /**
     * Persists a partial standup entry as a draft, allowing users to return and finish it later.
     */
    @Transactional
    public StandupResponse saveDraft(UUID userId, SaveDraftRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        LocalDate today = LocalDate.now();
        Standup standup = standupRepository
                .findByUserIdAndStandupDate(userId, today)
                .filter(s -> s.getStatus() == StandupStatus.DRAFT)
                .orElseGet(() -> Standup.builder()
                        .user(user)
                        .team(user.getTeam())
                        .standupDate(today)
                        .status(StandupStatus.DRAFT)
                        .build());

        if (standup.getStatus() == StandupStatus.SUBMITTED) {
            throw new BusinessException("Standup already submitted for today");
        }

        standup.setYesterdayText(request.yesterdayText());
        standup.setTodayText(request.todayText());
        standup.setBlockersText(request.blockersText());

        return StandupResponse.from(standupRepository.save(standup));
    }

    /**
     * Finds a specific standup by its unique identifier.
     */
    @Transactional(readOnly = true)
    public Standup findById(UUID id) {
        return standupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Standup", id));
    }

    /**
     * Retrieves the standup entry for a specific user on a given date.
     */
    @Transactional(readOnly = true)
    public StandupResponse getMyStandup(UUID userId, LocalDate date) {
        return standupRepository.findByUserIdAndStandupDate(userId, date)
                .map(StandupResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Not standup found for " + date));
    }

    /**
     * Compiles a comprehensive summary of all team standups for a specific day, including blockers and participation rates.
     */
    @Transactional(readOnly = true)
    public TeamStandupSummary getTeamSummary(UUID teamId, LocalDate date) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));

        List<User> allMembers = userRepository.findAllByTeamIdAndActiveTrue(team.getId());
        List<Standup> submitted = standupRepository.findSubmittedByTeamAndDate(team.getId(), date);
        List<Standup> blockers = standupRepository.findBlockersByTeamAndDate(team.getId(), date);

        Set<UUID> submittedIds = submitted.stream()
                .map(s -> s.getUser().getId())
                .collect(Collectors.toSet());

        List<String> missing = allMembers.stream()
                .filter(u -> !submittedIds.contains(u.getId()))
                .map(User::getName)
                .toList();

        double rate = allMembers.isEmpty()
                ? 0.0
                : (double) submitted.size() / allMembers.size() * 100;

        return new TeamStandupSummary(
                date,
                allMembers.size(),
                submitted.size(),
                allMembers.size() - submitted.size(),
                missing,
                submitted.stream().map(StandupResponse::from).toList(),
                blockers.stream().map(StandupResponse::from).toList(),
                Math.round(rate * 10.0) / 10.0
        );
    }

    /**
     * Fetches a paginated history of a user's past standup submissions.
     */
    @Transactional(readOnly = true)
    public List<StandupResponse> getMyHistory(UUID userId, int page, int size) {
        return standupRepository
                .findByUserIdOrderByStandupDateDesc(userId,
                        PageRequest.of(page, size, Sort.by("standupDate").descending()))
                .getContent()
                .stream()
                .map(StandupResponse::from)
                .toList();
    }

    /**
     * Broadcasts live updates on standup submission status to team members via WebSocket.
     */
    private void pushStatusUpdate(Standup standup) {
        UUID teamId = standup.getTeam().getId();
        long count = standupRepository.countSubmittedByTeamAndDate(teamId, standup.getStandupDate());
        int total = userRepository.findAllByTeamIdAndActiveTrue(teamId).size();

        var update = new StandupStatusUpdate(
                teamId,
                standup.getStandupDate(),
                standup.getUser().getId(),
                standup.getUser().getName(),
                true,
                (int) count,
                total
        );

        simpMessagingTemplate.convertAndSend(
                "/topic/teams" + teamId + "/standup", update
        );
    }

    /**
     * Links supporting documents or images to a standup entry, restricted by team deadlines.
     */
    @Transactional
    public AttachmentResponse addAttachment(UUID standupId, UUID userId,
                                            AddAttachmentRequest request) {
        Standup standup = standupRepository.findById(standupId)
                .orElseThrow(() -> new ResourceNotFoundException("Standup", standupId));
        if (!standup.getUser().getId().equals(userId)) {
            throw new BusinessException("You can only add attachments to your own standup");
        }
        var teamZone = safeZone(standup.getTeam().getTimezone());
        var teamNow = ZonedDateTime.now(teamZone).toLocalTime();
        if (teamNow.isAfter(standup.getTeam().getStandupDeadlineTime())) {
            throw new BusinessException("Cannot add attachments after the standup deadline");
        }

        if (request.attachmentType() == AttachmentType.IMAGE
                && (request.thumbnailUrl() == null || request.thumbnailUrl().isEmpty())) {
            throw new BusinessException("Thumbnail url is required for IMAGE attachments");
        }

        StandupAttachment attachment = StandupAttachment.builder()
                .standup(standup)
                .attachmentType(request.attachmentType())
                .url(request.url())
                .label(request.label() != null ? request.label() : request.url())
                .thumbnailUrl(request.thumbnailUrl())
                .build();

        return AttachmentResponse.from(standupAttachmentRepository.save(attachment));
    }

    /**
     * Removes an attachment from a standup, ensuring only the owner can delete their files.
     */
    @Transactional
    public void removeAttachment(UUID standupId, UUID attachmentId, UUID userId) {
        Standup standup = standupRepository.findById(standupId)
                .orElseThrow(() -> new ResourceNotFoundException("Standup", standupId));
        if (!standup.getUser().getId().equals(userId)) {
            throw new BusinessException("You can only remove attachments from your own standup");
        }

        StandupAttachment standupAttachment = standupAttachmentRepository
                .findByIdAndStandupId(attachmentId, standupId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));
        standupAttachmentRepository.delete(standupAttachment);
    }

    /**
     * Lists all files attached to a specific standup entry.
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(UUID standupId) {
        return standupAttachmentRepository.findAllByStandupIdOrderByCreatedAtAsc(standupId)
                .stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    /**
     * Resolves a timezone ID safely, defaulting to UTC if the provided zone is invalid.
     */
    private ZoneId safeZone(String timezone) {
        try {
            return (timezone != null && !timezone.isBlank())
                    ? ZoneId.of(timezone) : ZoneId.of("UTC");
        } catch (Exception ex) {
            log.warn("Invalid timezone '{}', falling back to UTC", timezone);
            return ZoneId.of("UTC");
        }
    }

}
