package com.niyiment.agilebutler.decision.model;


import com.niyiment.agilebutler.common.model.BaseEntity;
import com.niyiment.agilebutler.user.model.User;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "votes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_vote_session_user_option",
                columnNames = {"session_id", "user_id", "option_id"}
        ),
        indexes = {
                @Index(name = "idx_vote_session", columnList = "session_id"),
                @Index(name = "idx_vote_user_session", columnList = "user_id, session_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private DecisionSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private DecisionOption option;

    @Column(name = "comment_text", columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "rank_order")
    private Integer rankOrder;
}