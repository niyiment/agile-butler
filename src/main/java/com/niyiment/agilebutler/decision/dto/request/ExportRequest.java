package com.niyiment.agilebutler.decision.dto.request;

import com.niyiment.agilebutler.decision.model.enums.ExportFormat;

public record ExportRequest(ExportFormat format) {
}
