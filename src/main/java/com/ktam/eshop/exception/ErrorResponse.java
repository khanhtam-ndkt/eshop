package com.ktam.eshop.exception;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error body returned by the global exception handler.
 *
 * @param timestamp when the error occurred
 * @param status    HTTP status code
 * @param error     HTTP status reason phrase
 * @param message   human-readable summary
 * @param path      request path that caused the error
 * @param fieldErrors per-field validation messages, empty unless this is a validation error
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {}
}