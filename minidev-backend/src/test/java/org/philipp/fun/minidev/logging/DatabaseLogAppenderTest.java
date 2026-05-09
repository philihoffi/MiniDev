package org.philipp.fun.minidev.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DatabaseLogAppenderTest {

    @BeforeEach
    void resetSchemaFlag() {
        AtomicBoolean schemaReady = (AtomicBoolean) ReflectionTestUtils.getField(DatabaseLogAppender.class, "SCHEMA_READY");
        assertThat(schemaReady).isNotNull();
        schemaReady.set(false);
    }

    @Test
    void appendWritesLogEventToDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ILoggingEvent event = mock(ILoggingEvent.class);

        when(event.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(event.getLevel()).thenReturn(Level.INFO);
        when(event.getLoggerName()).thenReturn("org.test.Logger");
        when(event.getThreadName()).thenReturn("main");
        when(event.getFormattedMessage()).thenReturn("hello world");

        Map<String, String> mdc = new HashMap<>();
        mdc.put("requestId", "req-1");
        mdc.put("username", "user");
        mdc.put("path", "/api/test");
        when(event.getMDCPropertyMap()).thenReturn(mdc);

        TestableDatabaseLogAppender appender = new TestableDatabaseLogAppender();
        ReflectionTestUtils.setField(appender, "jdbcTemplate", jdbcTemplate);
        appender.start();

        appender.doAppend(event);

        verify(jdbcTemplate, atLeastOnce()).execute(anyString());
        verify(jdbcTemplate).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        assertThat(appender.errorCount).isZero();
    }

    @Test
    void appendReportsAppenderErrorsViaAddError() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ILoggingEvent event = mock(ILoggingEvent.class);

        when(event.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(event.getLevel()).thenReturn(Level.INFO);
        when(event.getLoggerName()).thenReturn("org.test.Logger");
        when(event.getThreadName()).thenReturn("main");
        when(event.getFormattedMessage()).thenReturn("hello world");
        when(event.getMDCPropertyMap()).thenReturn(Map.of());

        doThrow(new RuntimeException("db down")).when(jdbcTemplate).execute(anyString());

        TestableDatabaseLogAppender appender = new TestableDatabaseLogAppender();
        ReflectionTestUtils.setField(appender, "jdbcTemplate", jdbcTemplate);
        appender.start();

        appender.doAppend(event);

        assertThat(appender.errorCount).isEqualTo(1);
        assertThat(appender.lastErrorMessage).contains("database_log_append_failed");
        assertThat(appender.lastError).isNotNull();
    }

    private static class TestableDatabaseLogAppender extends DatabaseLogAppender {
        private int errorCount;
        private String lastErrorMessage;
        private Throwable lastError;

        @Override
        public void addError(String msg, Throwable ex) {
            errorCount++;
            lastErrorMessage = msg;
            lastError = ex;
        }
    }
}

