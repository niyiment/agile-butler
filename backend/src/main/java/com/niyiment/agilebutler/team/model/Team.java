package com.niyiment.agilebutler.team.model;


import com.niyiment.agilebutler.common.model.BaseEntity;
import com.niyiment.agilebutler.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * The time by which all standup submissions must be in.
     * The aggregation job runs at this time.
     */
    @Column(name = "standup_deadline_time", nullable = false)
    @Builder.Default
    private LocalTime standupDeadlineTime = LocalTime.of(11, 0);

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "invite_code", unique = true, length = 12)
    private String inviteCode;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    @Builder.Default
    private List<User> members = new ArrayList<>();
}