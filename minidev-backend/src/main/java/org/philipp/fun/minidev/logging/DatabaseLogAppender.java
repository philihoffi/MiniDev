package org.philipp.fun.minidev.logging;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.philipp.fun.minidev.common.util.SpringContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;

/**
 * Logback appender that persists log events to a database table.
 */
public class DatabaseLogAppender extends AppenderBase<ILoggingEvent> {

    /** Maximum length for VARCHAR columns. */
    private static final int MAX_VARCHAR = 255;

    /** Maximum length for TEXT columns. */
    private static final int MAX_TEXT = 8000;

    /** Flag indicating the database schema has been initialized. */
    private static final AtomicBoolean SCHEMA_READY = new AtomicBoolean(false);

    /** Guard against recursive logging from within the appender. */
    private static final ThreadLocal<Boolean> IN_APPENDER =
            ThreadLocal.withInitial(() -> false);

    /** The cached JdbcTemplate instance. */
    private volatile JdbcTemplate jdbcTemplate;

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (Boolean.TRUE.equals(IN_APPENDER.get())) {
            return;
        }

        IN_APPENDER.set(true);
        try {
            JdbcTemplate template = resolveJdbcTemplate();
            if (template == null) {
                return;
            }

            ensureSchema(template);

            String exceptionText =
                    extractException(eventObject.getThrowableProxy());
            template.update(
                    "INSERT INTO application_logs"
                    + " (created_at, level, logger, thread, message,"
                    + " exception, request_id, username, path)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Timestamp.from(
                            Instant.ofEpochMilli(
                                    eventObject.getTimeStamp())),
                    trim(eventObject.getLevel().toString()),
                    trim(eventObject.getLoggerName()),
                    trim(eventObject.getThreadName()),
                    trimText(eventObject.getFormattedMessage()),
                    trimText(exceptionText),
                    trim(mdcValue(eventObject, "requestId")),
                    trim(mdcValue(eventObject, "username")),
                    trim(mdcValue(eventObject, "path"))
            );
        } catch (Exception e) {
            addError("database_log_append_failed", e);
        } finally {
            IN_APPENDER.remove();
        }
    }

    /**
     * Resolves the JdbcTemplate, caching it once obtained.
     *
     * @return the JdbcTemplate, or null if not available
     */
    private JdbcTemplate resolveJdbcTemplate() {
        JdbcTemplate template = jdbcTemplate;
        if (template != null) {
            return template;
        }

        template = SpringContextHolder.getBean(JdbcTemplate.class);
        if (template != null) {
            jdbcTemplate = template;
        }
        return template;
    }

    /**
     * Ensures the application_logs table exists.
     *
     * @param template the JdbcTemplate
     */
    private void ensureSchema(JdbcTemplate template) {
        if (SCHEMA_READY.get()) {
            return;
        }

        template.execute(
                """
                CREATE TABLE IF NOT EXISTS application_logs (
                    id BIGSERIAL PRIMARY KEY,
                    created_at TIMESTAMPTZ NOT NULL,
                    level VARCHAR(32) NOT NULL,
                    logger VARCHAR(255) NOT NULL,
                    thread VARCHAR(255) NOT NULL,
                    message TEXT,
                    exception TEXT,
                    request_id VARCHAR(255),
                    username VARCHAR(255),
                    path VARCHAR(255)
                )
                """
        );
        template.execute(
                "CREATE INDEX IF NOT EXISTS idx_application_logs_created_at"
                + " ON application_logs (created_at DESC)");
        SCHEMA_READY.set(true);
    }

    /**
     * Extracts exception text from a throwable proxy.
     *
     * @param throwableProxy the throwable proxy
     * @return the exception string, or null
     */
    private String extractException(IThrowableProxy throwableProxy) {
        if (throwableProxy == null) {
            return null;
        }
        return ThrowableProxyUtil.asString(throwableProxy);
    }

    /**
     * Truncates a value to the maximum VARCHAR length.
     *
     * @param value the value to truncate
     * @return the truncated value, or null
     */
    private String trim(String value) {
        if (value == null || value.length() <= MAX_VARCHAR) {
            return value;
        }
        return value.substring(0, MAX_VARCHAR);
    }

    /**
     * Truncates a value to the maximum TEXT length.
     *
     * @param value the value to truncate
     * @return the truncated value, or null
     */
    private String trimText(String value) {
        if (value == null || value.length() <= MAX_TEXT) {
            return value;
        }
        return value.substring(0, MAX_TEXT);
    }

    /**
     * Reads an MDC value from the logging event.
     *
     * @param eventObject the logging event
     * @param key         the MDC key
     * @return the MDC value, or null
     */
    private String mdcValue(ILoggingEvent eventObject, String key) {
        if (eventObject.getMDCPropertyMap() == null) {
            return null;
        }
        return eventObject.getMDCPropertyMap().get(key);
    }
}