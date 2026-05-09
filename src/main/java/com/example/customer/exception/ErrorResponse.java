package com.example.customer.exception;

import java.time.LocalDateTime;

/**
 * Standard error envelope returned by {@link GlobalExceptionHandler} for failed requests.
 *
 * @param message   human-readable failure description
 * @param status    HTTP status code numeric value
 * @param timestamp server-side timestamp at which the error was produced
 */
public record ErrorResponse(
        String message,
        int status,
        LocalDateTime timestamp
) {
}
