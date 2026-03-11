package com.niyiment.agilebutler.standup.controller;

import com.niyiment.agilebutler.common.model.ApiResponse;
import com.niyiment.agilebutler.standup.dto.request.AddAttachmentRequest;
import com.niyiment.agilebutler.standup.dto.request.SaveDraftRequest;
import com.niyiment.agilebutler.standup.dto.request.SubmitStandupRequest;
import com.niyiment.agilebutler.standup.dto.response.AttachmentResponse;
import com.niyiment.agilebutler.standup.dto.response.StandupResponse;
import com.niyiment.agilebutler.standup.dto.response.TeamStandupSummary;
import com.niyiment.agilebutler.standup.service.StandupService;
import com.niyiment.agilebutler.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/standups")
@RequiredArgsConstructor
@Tag(name = "Standups", description = "Daily standup submission and aggregation")
public class StandupController {

    private final StandupService standupService;

    @PostMapping
    @Operation(summary = "Submit today's standup update")
    public ResponseEntity<ApiResponse<StandupResponse>> submit(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SubmitStandupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(standupService.submitStandup(user.getId(), request)));
    }

    @PostMapping("/draft")
    @Operation(summary = "Save a standup draft")
    public ResponseEntity<ApiResponse<StandupResponse>> saveDraft(
            @AuthenticationPrincipal User user,
            @RequestBody SaveDraftRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(standupService.saveDraft(user.getId(), request)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my standup for a specific date")
    public ResponseEntity<ApiResponse<StandupResponse>> myStandup(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().toString()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(standupService.getMyStandup(user.getId(), date)));
    }

    @GetMapping("/me/history")
    @Operation(summary = "Get my standup history")
    public ResponseEntity<ApiResponse<List<StandupResponse>>> history(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(standupService.getMyHistory(user.getId(), page, size)));
    }

    @GetMapping("/teams/{teamId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'SCRUM_MASTER')")
    @Operation(summary = "Get aggregated team standup summary for a given date (manager view)")
    public ResponseEntity<ApiResponse<TeamStandupSummary>> teamSummary(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().toString()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(standupService.getTeamSummary(teamId, date)));
    }

    @PostMapping("/{standupId}/attachments")
    @Operation(summary = "Add a link or screenshot attachment to a standup")
    public ResponseEntity<ApiResponse<AttachmentResponse>> addAttachment(
            @AuthenticationPrincipal User user,
            @PathVariable UUID standupId,
            @Valid @RequestBody AddAttachmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(standupService.addAttachment(user.getId(), standupId, request)));
    }

    @GetMapping("/{standupId}/attachments")
    @Operation(summary = "Get all attachments for a standup")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getAttachments(
            @PathVariable UUID standupId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(standupService.getAttachments(standupId)));
    }

    @DeleteMapping("/{standupId}/attachments/{attachmentId}")
    @Operation(summary = "Remove an attachment from a standup")
    public ResponseEntity<ApiResponse<AttachmentResponse>> removeAttachment(
            @AuthenticationPrincipal User user,
            @PathVariable UUID standupId,
            @PathVariable UUID attachmentId
    ) {
        standupService.removeAttachment(standupId, attachmentId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Attachment removed", null));
    }

}