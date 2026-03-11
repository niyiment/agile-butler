package com.niyiment.agilebutler.standup.dto.request;

import com.niyiment.agilebutler.standup.model.enums.AttachmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record AddAttachmentRequest(
        @NotNull
        AttachmentType attachmentType,
        @NotBlank
        @URL
        @Size(max = 2048)
        String url,
        @Size(max = 200)
        String label,
        @Size(max = 2048)
        String thumbnailUrl
) {
}
