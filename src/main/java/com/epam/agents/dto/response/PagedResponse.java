package com.epam.agents.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Generic paginated response wrapper that maps a Spring {@link Page} to a JSON-friendly DTO.
 *
 * @param <T>
 *            the type of elements in the page
 * @param content
 *            the list of items on the current page
 * @param page
 *            current page number (0-based)
 * @param size
 *            number of items per page
 * @param totalElements
 *            total number of items across all pages
 * @param totalPages
 *            total number of pages
 * @param first
 *            {@code true} if this is the first page
 * @param last
 *            {@code true} if this is the last page
 */
public record PagedResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last) {

    /**
     * Factory method — converts a Spring {@link Page} into a {@link PagedResponse}.
     *
     * @param <T>
     *            the element type
     * @param springPage
     *            the Spring Data page result
     * @return a new {@code PagedResponse} populated from the given page
     */
    public static <T> PagedResponse<T> from(Page<T> springPage) {
        return new PagedResponse<>(springPage.getContent(), springPage.getNumber(), springPage.getSize(), springPage.getTotalElements(), springPage.getTotalPages(), springPage.isFirst(), springPage.isLast());
    }
}
