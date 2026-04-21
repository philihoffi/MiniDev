package org.philipp.fun.minidev.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_request_logs")
public class ApiRequestLog extends BaseEntity {

    private String method;
    private String uri;
    private String clientIp;
    private String username;
    private int status;
    private long duration;
    private LocalDateTime timestamp;

    public ApiRequestLog() {
    }

    public ApiRequestLog(String method, String uri, String clientIp, String username, int status, long duration) {
        this.method = method;
        this.uri = uri;
        this.clientIp = clientIp;
        this.username = username;
        this.status = status;
        this.duration = duration;
        this.timestamp = LocalDateTime.now();
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
