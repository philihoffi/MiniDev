package org.philipp.fun.minidev.common.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.philipp.fun.minidev.common.dto.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Global exception handler for REST controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles ResourceNotFoundException.
     *
     * @param exception the exception
     * @param request   the HTTP request
     * @return error response entity
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        LOG.warn("resource_not_found method={} path={} message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        ));
    }

    /**
     * Handles DuplicateResourceException.
     *
     * @param exception the exception
     * @param request   the HTTP request
     * @return error response entity
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResource(
            DuplicateResourceException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.CONFLICT;
        LOG.warn("duplicate_resource method={} path={} message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        ));
    }

    /**
     * Handles BadRequestException.
     *
     * @param exception the exception
     * @param request   the HTTP request
     * @return error response entity
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            BadRequestException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        LOG.warn("bad_request method={} path={} message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        ));
    }

    /**
     * Handles MethodArgumentNotValidException.
     *
     * @param exception the exception
     * @param request   the HTTP request
     * @return error response entity
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        HttpStatus status = HttpStatus.BAD_REQUEST;
        LOG.warn("validation_failed method={} path={} fieldErrors={}",
                request.getMethod(), request.getRequestURI(), validationErrors.size());
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Validation failed",
                request.getRequestURI(),
                validationErrors
        ));
    }

    /**
     * Handles ConstraintViolationException.
     *
     * @param exception the exception
     * @param request   the HTTP request
     * @return error response entity
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        LOG.warn("constraint_violation method={} path={} message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        ));
    }

    /**
     * Handles DataIntegrityViolationException.
     *
     * @param exception the exception
     * @param request   the HTTP request
     * @return error response entity
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.CONFLICT;
        String message = exception.getMostSpecificCause() != null
                ? exception.getMostSpecificCause().getMessage()
                : "Data integrity violation";
        LOG.warn("data_integrity_violation method={} path={} message={}",
                request.getMethod(), request.getRequestURI(), message);
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Map.of()
        ));
    }

    /**
     * Handles ResponseStatusException.
     *
     * @param exception the exception
     * @param request   the HTTP request
     * @return error response entity
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() != null ? exception.getReason() : status.getReasonPhrase();
        LOG.warn("response_status_exception method={} path={} status={} message={}",
                request.getMethod(), request.getRequestURI(), status.value(), message);

        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Map.of()
        ));
    }

    /**
     * Handles generic Exception.
     *
     * @param exception the exception
     * @param request   the HTTP request
     * @return error response entity
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        LOG.error("unhandled_exception method={} path={}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage() != null ? exception.getMessage() : "Internal server error",
                request.getRequestURI(),
                Map.of()
        ));
    }
}