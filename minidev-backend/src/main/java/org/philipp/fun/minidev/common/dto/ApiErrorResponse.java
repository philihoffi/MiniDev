package org.philipp.fun.minidev.common.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Standard API error response DTO.
 *
 * @param timestamp        the time at which the error occurred
 * @param status           the HTTP status code
 * @param error            the error type description
 * @param message          the human-readable error message
 * @param path             the request path that caused the error
 * @param validationErrors field-level validation errors, if any
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
}