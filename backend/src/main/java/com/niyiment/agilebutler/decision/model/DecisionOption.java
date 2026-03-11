package com.niyiment.agilebutler.decision.model;

import com.niyiment.agilebutler.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "decision_options")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private DecisionSession session;

    @Column(name = "option_text", nullable = false, length = 300)
    private String optionText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @OneToMany(mappedBy = "option", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<Vote> votes = new java.util.ArrayList<>();
}
