package com.niyiment.agilebutler.standup.model;

import com.niyiment.agilebutler.common.model.BaseEntity;
import com.niyiment.agilebutler.standup.model.enums.AttachmentType;
import jakarta.persistence.*;
import lombok.*;

/**
 * An attachment associated with a standup entry.
 */
@Entity
@Table(name = "standup_attachments",
        indexes = @Index(name = "idx_attachment_standup", columnList = "standup_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandupAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "standup_id", nullable = false)
    private Standup standup;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 10)
    private AttachmentType attachmentType;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(length = 200)
    private String label;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;
}
