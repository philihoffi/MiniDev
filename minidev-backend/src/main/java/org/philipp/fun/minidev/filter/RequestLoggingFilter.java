package org.philipp.fun.minidev.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.philipp.fun.minidev.model.ApiRequestLog;
import org.philipp.fun.minidev.repository.ApiRequestLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final int MAX_REQUEST_ID_LENGTH = 128;
    private static final Pattern SAFE_REQUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final ApiRequestLogRepository requestLogRepository;

    public RequestLoggingFilter(ApiRequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
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

        log.info("api_request_started method={} uri={} clientIp={}", request.getMethod(), uri, request.getRemoteAddr());

        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                long duration = System.currentTimeMillis() - startTime;

                String method = request.getMethod();
                String clientIp = request.getRemoteAddr();
                int status = response.getStatus();

                String username = "anonymous";
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                    username = auth.getName();
                }

                MDC.put("username", username);

                log.info("api_request method={} uri={} status={} durationMs={} username={} clientIp={}",
                        method, uri, status, duration, username, clientIp);

                ApiRequestLog apiRequestLog = new ApiRequestLog(method, uri, clientIp, username, status, duration);
                requestLogRepository.save(apiRequestLog);
            } catch (Exception e) {
                log.warn("api_request_persist_failed uri={} reason={}", uri, e.getMessage(), e);
            } finally {
                MDC.remove("username");
                MDC.remove("path");
                MDC.remove("requestId");
            }
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incomingRequestId = request.getHeader(REQUEST_ID_HEADER);
        if (!isSafeRequestId(incomingRequestId)) {
            return UUID.randomUUID().toString();
        }
        return incomingRequestId.trim();
    }

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
