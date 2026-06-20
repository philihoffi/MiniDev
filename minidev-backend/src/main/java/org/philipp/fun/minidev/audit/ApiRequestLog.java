package org.philipp.fun.minidev.audit;

import org.philipp.fun.minidev.model.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entity for persisting API request audit logs.
 */
@Entity
@Table(name = "api_request_logs")
public class ApiRequestLog extends BaseEntity {

    /** The HTTP method. */
    private String method;

    /** The request URI. */
    private String uri;

    /** The client IP address. */
    private String clientIp;

    /** The authenticated username. */
    private String username;

    /** The HTTP response status code. */
    private int status;

    /** The request duration in milliseconds. */
    private long duration;

    /** Default constructor. */
    public ApiRequestLog() {
    }

    /**
     * Constructs a new ApiRequestLog.
     *
     * @param method   the HTTP method
     * @param uri      the request URI
     * @param clientIp the client IP
     * @param username the username
     * @param status   the HTTP status
     * @param duration the duration in ms
     */
    public ApiRequestLog(
            String method, String uri, String clientIp,
            String username, int status, long duration) {
        this.method = method;
        this.uri = uri;
        this.clientIp = clientIp;
        this.username = username;
        this.status = status;
        this.duration = duration;
    }

    /**
     * Gets the HTTP method.
     *
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the HTTP method.
     *
     * @param method the method to set
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Gets the request URI.
     *
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the request URI.
     *
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Gets the client IP address.
     *
     * @return the client IP
     */
    public String getClientIp() {
        return clientIp;
    }

    /**
     * Sets the client IP address.
     *
     * @param clientIp the client IP to set
     */
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    /**
     * Gets the authenticated username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the authenticated username.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the HTTP response status code.
     *
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the HTTP response status code.
     *
     * @param status the status to set
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Gets the request duration in milliseconds.
     *
     * @return the duration
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Sets the request duration in milliseconds.
     *
     * @param duration the duration to set
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }
}