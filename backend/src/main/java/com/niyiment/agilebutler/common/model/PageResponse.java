package com.niyiment.agilebutler.common.model;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Flattens Spring's {@link Page} into a clean JSON payload.
 *
 * @param content
 * @param page
 * @param size
 * @param totalElements
 * @param totalPages
 * @param last
 * @param <T>
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        int totalElements,
        int totalPages,
        boolean last
) {

    public PageResponse(Page<T> page) {
        this(page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalPages(),
                page.isLast()
        );
    }

}
