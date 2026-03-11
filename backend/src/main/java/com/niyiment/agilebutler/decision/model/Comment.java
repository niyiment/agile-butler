package com.niyiment.agilebutler.decision.model;


import com.niyiment.agilebutler.common.model.BaseEntity;
import com.niyiment.agilebutler.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "comments",
        indexes = @Index(name = "idx_comment_session", columnList = "session_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private DecisionSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "parent_comment_id")
    private UUID parentCommentId;

    @Column(name = "reaction_count")
    @Builder.Default
    private int reactionCount = 0;

    @ElementCollection
    @CollectionTable(
            name = "comment_reactions",
            joinColumns = @JoinColumn(name = "comment_id")
    )
    @MapKeyColumn(name = "user_id")
    @Column(name = "emoji")
    @Builder.Default
    private Map<String, String> userReactions = new HashMap<>();
}