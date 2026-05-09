package org.philipp.fun.minidev.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.philipp.fun.minidev.model.ApiRequestLog;
import org.philipp.fun.minidev.repository.ApiRequestLogRepository;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RequestLoggingFilterTest {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testApiRequestIsLogged() throws Exception {
        // Arrange
        ApiRequestLogRepository repository = mock(ApiRequestLogRepository.class);
        RequestLoggingFilter filter = new RequestLoggingFilter(repository);
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Request-Id")).thenReturn("req-123");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getStatus()).thenReturn(200);
        
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(response).setHeader("X-Request-Id", "req-123");

            ArgumentCaptor<ApiRequestLog> captor = ArgumentCaptor.forClass(ApiRequestLog.class);
            verify(repository).save(captor.capture());

            ApiRequestLog log = captor.getValue();
            assertThat(log.getMethod()).isEqualTo("GET");
            assertThat(log.getUri()).isEqualTo("/api/test");
            assertThat(log.getUsername()).isEqualTo("testuser");
            assertThat(log.getStatus()).isEqualTo(200);

            List<ILoggingEvent> events = listAppender.list;
            assertThat(events).isNotEmpty();
            ILoggingEvent requestEvent = events.get(events.size() - 1);
            assertThat(requestEvent.getLevel()).isEqualTo(Level.INFO);
            assertThat(requestEvent.getFormattedMessage())
                    .contains("api_request")
                    .contains("method=GET")
                    .contains("uri=/api/test")
                    .contains("status=200")
                    .contains("username=testuser");
        } finally {
            logger.detachAppender(listAppender);
            listAppender.stop();
        }
    }

    @Test
    public void testNonApiRequestIsNotLogged() throws Exception {
        // Arrange
        ApiRequestLogRepository repository = mock(ApiRequestLogRepository.class);
        RequestLoggingFilter filter = new RequestLoggingFilter(repository);
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        
        when(request.getRequestURI()).thenReturn("/index.html");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(repository, never()).save(any());
    }

    @Test
    public void testUnsafeRequestIdHeaderIsReplacedWithGeneratedValue() throws Exception {
        ApiRequestLogRepository repository = mock(ApiRequestLogRepository.class);
        RequestLoggingFilter filter = new RequestLoggingFilter(repository);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Request-Id")).thenReturn("bad\r\nheader");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getStatus()).thenReturn(200);

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<String> requestIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Request-Id"), requestIdCaptor.capture());
        String generatedRequestId = requestIdCaptor.getValue();

        assertThat(generatedRequestId).doesNotContain("\r").doesNotContain("\n");
        assertThat(UUID_PATTERN.matcher(generatedRequestId).matches()).isTrue();
        verify(repository).save(any(ApiRequestLog.class));
    }
}
