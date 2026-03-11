package com.niyiment.agilebutler.decision.controller;

import com.niyiment.agilebutler.common.model.ApiResponse;
import com.niyiment.agilebutler.common.model.PageResponse;
import com.niyiment.agilebutler.decision.dto.request.*;
import com.niyiment.agilebutler.decision.dto.response.CommentResponse;
import com.niyiment.agilebutler.decision.dto.response.SessionResponse;
import com.niyiment.agilebutler.decision.dto.response.SessionSummary;
import com.niyiment.agilebutler.decision.model.enums.ExportFormat;
import com.niyiment.agilebutler.decision.service.DecisionSessionService;
import com.niyiment.agilebutler.decision.service.ExportService;
import com.niyiment.agilebutler.decision.service.VotingService;
import com.niyiment.agilebutler.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "Decision Sessions", description = "Real-time and async decision sessions")
public class DecisionController {

    private final DecisionSessionService sessionService;
    private final VotingService votingService;
    private final ExportService exportService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SCRUM_MASTER')")
    @Operation(summary = "Create a new decision session")
    public ResponseEntity<ApiResponse<SessionResponse>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(sessionService.createSession(user.getId(), request)));
    }

    @PostMapping("/from-blocker")
    @PreAuthorize("hasAnyRole('ADMIN', 'SCRUM_MASTER')")
    @Operation(summary = "Create a decision session from a standup blocker")
    public ResponseEntity<ApiResponse<SessionResponse>> createFromBlocker(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateFromBlockerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(sessionService.createFromBlocker(user.getId(), request)));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session details including live vote counts")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getSession(sessionId)));
    }

    @PostMapping("/{sessionId}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'SCRUM_MASTER')")
    @Operation(summary = "Close a session and record the result")
    public ResponseEntity<ApiResponse<SessionResponse>> close(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                sessionService.closeSession(sessionId, user.getId())));
    }

    @GetMapping("/teams/{teamId}/active")
    @Operation(summary = "Get all active sessions for a team")
    public ResponseEntity<ApiResponse<List<SessionSummary>>> activeSessions(
            @PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getActiveSessions(teamId)));
    }

    @GetMapping("/teams/{teamId}/history")
    @Operation(summary = "Get paginated session history for a team")
    public ResponseEntity<ApiResponse<PageResponse<SessionSummary>>> history(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                new PageResponse<>(sessionService.getSessionHistory(teamId, page, size))));
    }

    @PostMapping("/{sessionId}/vote")
    @Operation(summary = "Cast a vote in a session (single choice / yes-no / ranked)")
    public ResponseEntity<ApiResponse<Void>> vote(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CastVoteRequest request) {
        votingService.castVote(sessionId, user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Vote recorded", null));
    }

    @PostMapping("/{sessionId}/multi-vote")
    @Operation(summary = "Cast multiple votes (multiple choice sessions)")
    public ResponseEntity<ApiResponse<Void>> multiVote(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CastMultiVoteRequest request) {
        votingService.castMultiVote(sessionId, user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Votes recorded", null));
    }

    @PostMapping("/{sessionId}/comments")
    @Operation(summary = "Post a comment in a session")
    public ResponseEntity<ApiResponse<CommentResponse>> comment(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AddCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        votingService.addComment(sessionId, user.getId(), request)));
    }

    @GetMapping("/{sessionId}/comments")
    @Operation(summary = "Get all comments for a session")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(votingService.getComments(sessionId)));
    }

    @PostMapping("/{sessionId}/comments/{commentId}/reactions")
    @Operation(summary = "React to a comment with an emoji - toggles on/off")
    public ResponseEntity<ApiResponse<CommentResponse>> react(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId,
            @PathVariable UUID commentId,
            @Valid @RequestBody ReactToCommentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                votingService.reactToComment(sessionId, commentId, user.getId(), request)
        ));
    }

    public ResponseEntity<byte[]> export(
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = "MARKDOWN") ExportFormat format
    ) {
        return switch (format) {
            case PDF -> {
                byte[] pdf = exportService.exportAsPdf(sessionId);
                yield ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment()
                                        .filename("session-" + sessionId + ".pdf")
                                        .build().toString())
                        .body(pdf);
            }
            case MARKDOWN -> {
                String md = exportService.exportAsMarkdown(sessionId);
                byte[] bytes = md.getBytes(StandardCharsets.UTF_8);
                yield ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/markdown;" +
                                "charset=utf-8"))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment()
                                        .filename("session-" + sessionId + ".md")
                                        .build().toString())
                        .body(bytes);
            }
        };
    }
}
