package com.niyiment.agilebutler.decision.model;

import com.niyiment.agilebutler.common.model.BaseEntity;
import com.niyiment.agilebutler.decision.model.enums.DecisionType;
import com.niyiment.agilebutler.decision.model.enums.SessionStatus;
import com.niyiment.agilebutler.decision.model.enums.SessionType;
import com.niyiment.agilebutler.team.model.Team;
import com.niyiment.agilebutler.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "decision_sessions",
        indexes = {
                @Index(name = "idx_session_team_status", columnList = "team_id, status"),
                @Index(name = "idx_session_created_by", columnList = "created_by_user_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionSession extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    private SessionType sessionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 20)
    private DecisionType decisionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "is_anonymous", nullable = false)
    @Builder.Default
    private boolean anonymous = false;

    @Column(name = "max_duration_minutes")
    private Integer maxDurationMinutes;

    @Column(name = "closes_at")
    private Instant closesAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(length = 50)
    private String category;        // technical | process | planning

    @Column(name = "linked_standup_id")
    private UUID linkedStandupId;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<DecisionOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<Vote> votes = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    public boolean isOpen() {
        return status == SessionStatus.ACTIVE;
    }
}