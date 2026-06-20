package org.philipp.fun.minidev.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when the client sends a malformed or invalid request.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    /**
     * Constructs a new exception with the given detail message.
     *
     * @param message the detail message
     */
    public BadRequestException(String message) {
        super(message);
    }
}