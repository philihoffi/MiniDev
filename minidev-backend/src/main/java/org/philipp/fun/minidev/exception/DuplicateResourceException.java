package org.philipp.fun.minidev.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an attempt is made to create a resource that already
 * exists.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    /**
     * Constructs a new exception with the given detail message.
     *
     * @param message the detail message
     */
    public DuplicateResourceException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception describing a duplicate resource.
     *
     * @param resource the resource type name
     * @param field    the field that caused the conflict
     * @param value    the conflicting value
     */
    public DuplicateResourceException(String resource, String field, String value) {
        super(resource + " with " + field + " '" + value + "' already exists");
    }
}