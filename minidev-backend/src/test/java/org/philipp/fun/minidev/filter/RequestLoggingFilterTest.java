package org.philipp.fun.minidev.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.philipp.fun.minidev.model.ApiRequestLog;
import org.philipp.fun.minidev.repository.ApiRequestLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RequestLoggingFilterTest {

    @Test
    public void testApiRequestIsLogged() throws Exception {
        // Arrange
        ApiRequestLogRepository repository = mock(ApiRequestLogRepository.class);
        RequestLoggingFilter filter = new RequestLoggingFilter(repository);
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getStatus()).thenReturn(200);
        
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        
        ArgumentCaptor<ApiRequestLog> captor = ArgumentCaptor.forClass(ApiRequestLog.class);
        verify(repository).save(captor.capture());
        
        ApiRequestLog log = captor.getValue();
        assertThat(log.getMethod()).isEqualTo("GET");
        assertThat(log.getUri()).isEqualTo("/api/test");
        assertThat(log.getUsername()).isEqualTo("testuser");
        assertThat(log.getStatus()).isEqualTo(200);
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
}
