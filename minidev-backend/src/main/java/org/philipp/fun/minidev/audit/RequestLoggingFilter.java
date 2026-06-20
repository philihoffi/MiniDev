package org.philipp.fun.minidev.audit;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that logs API requests and persists audit records to the database.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    /** The HTTP header name for request IDs. */
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    /** Maximum allowed length for a request ID. */
    private static final int MAX_REQUEST_ID_LENGTH = 128;

    /** Pattern for safe request ID characters. */
    private static final Pattern SAFE_REQUEST_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9._-]+$");

    /** Logger. */
    private static final Logger LOG =
            LoggerFactory.getLogger(RequestLoggingFilter.class);

    /** Repository for persisting API request logs. */
    private final ApiRequestLogRepository requestLogRepository;

    /**
     * Constructs a new RequestLoggingFilter.
     *
     * @param requestLogRepository the API request log repository
     */
    public RequestLoggingFilter(
            ApiRequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (!uri.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        String requestId = resolveRequestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        MDC.put("requestId", requestId);
        MDC.put("path", uri);

        LOG.info(
                "api_request_started method={} uri={} clientIp={}",
                request.getMethod(), uri, request.getRemoteAddr());

        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                long duration = System.currentTimeMillis() - startTime;

                String method = request.getMethod();
                String clientIp = request.getRemoteAddr();
                int status = response.getStatus();

                String username = "anonymous";
                Authentication auth =
                        SecurityContextHolder.getContext()
                                .getAuthentication();
                if (auth != null
                        && auth.isAuthenticated()
                        && !"anonymousUser".equals(auth.getName())) {
                    username = auth.getName();
                }

                MDC.put("username", username);

                LOG.info(
                        "api_request method={} uri={} status={}"
                        + " durationMs={} username={} clientIp={}",
                        method, uri, status, duration, username, clientIp);

                ApiRequestLog apiRequestLog = new ApiRequestLog(
                        method, uri, clientIp, username, status, duration);
                requestLogRepository.save(apiRequestLog);
            } catch (Exception e) {
                LOG.warn(
                        "api_request_persist_failed uri={} reason={}",
                        uri, e.getMessage(), e);
            } finally {
                MDC.remove("username");
                MDC.remove("path");
                MDC.remove("requestId");
            }
        }
    }

    /**
     * Resolves the request ID from the incoming header or generates a new one.
     *
     * @param request the HTTP request
     * @return the request ID
     */
    private String resolveRequestId(HttpServletRequest request) {
        String incomingRequestId = request.getHeader(REQUEST_ID_HEADER);
        if (!isSafeRequestId(incomingRequestId)) {
            return UUID.randomUUID().toString();
        }
        return incomingRequestId.trim();
    }

    /**
     * Validates whether a request ID is safe to use.
     *
     * @param requestId the request ID to validate
     * @return true if the request ID is safe
     */
    private boolean isSafeRequestId(String requestId) {
        if (requestId == null) {
            return false;
        }

        String trimmed = requestId.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_REQUEST_ID_LENGTH) {
            return false;
        }

        return SAFE_REQUEST_ID_PATTERN.matcher(trimmed).matches();
    }
}