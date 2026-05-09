package com.example.customer.dto;

import java.util.List;

/**
 * Generic cursor-based page envelope returned by list endpoints.
 *
 * @param <T>        the element type carried in {@code content}
 * @param content    the slice of records for the current page
 * @param nextCursor opaque cursor to fetch the next page; {@code null} when no more data
 * @param hasMore    {@code true} if at least one more page is available after this one
 */
public record CursorPageResponse<T>(
        List<T> content,
        String nextCursor,
        boolean hasMore
) {
}
