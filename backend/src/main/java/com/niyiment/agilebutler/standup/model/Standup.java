package com.niyiment.agilebutler.standup.model;


import com.niyiment.agilebutler.common.model.BaseEntity;
import com.niyiment.agilebutler.standup.model.enums.StandupStatus;
import com.niyiment.agilebutler.team.model.Team;
import com.niyiment.agilebutler.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "standups",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_standup_user_date",
                columnNames = {"user_id", "standup_date"}
        ),
        indexes = {
                @Index(name = "idx_standup_team_date", columnList = "team_id, standup_date"),
                @Index(name = "idx_standup_user_date", columnList = "user_id, standup_date")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Standup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "standup_date", nullable = false)
    private LocalDate standupDate;

    @Column(name = "yesterday_text", columnDefinition = "TEXT")
    private String yesterdayText;

    @Column(name = "today_text", columnDefinition = "TEXT")
    private String todayText;

    @Column(name = "blockers_text", columnDefinition = "TEXT")
    private String blockersText;

    @Column(name = "is_blocker_flagged", nullable = false)
    @Builder.Default
    private boolean blockerFlagged = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StandupStatus status = StandupStatus.SUBMITTED;

    @OneToMany(mappedBy = "standup", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<StandupAttachment> attachments = new ArrayList<>();

    public boolean hasBlocker() {
        return blockersText != null && !blockersText.isBlank();
    }

}
