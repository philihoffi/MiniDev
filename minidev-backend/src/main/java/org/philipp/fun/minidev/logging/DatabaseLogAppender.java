package org.philipp.fun.minidev.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import org.philipp.fun.minidev.config.SpringContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseLogAppender extends AppenderBase<ILoggingEvent> {

    private static final int MAX_VARCHAR = 255;
    private static final int MAX_TEXT = 8000;
    private static final AtomicBoolean SCHEMA_READY = new AtomicBoolean(false);
    private static final ThreadLocal<Boolean> IN_APPENDER = ThreadLocal.withInitial(() -> false);

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

            String exceptionText = extractException(eventObject.getThrowableProxy());
            template.update(
                    "INSERT INTO application_logs (created_at, level, logger, thread, message, exception, request_id, username, path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Timestamp.from(Instant.ofEpochMilli(eventObject.getTimeStamp())),
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
            // Report via Logback status system (not SLF4J) to avoid recursive appender logging.
            addError("database_log_append_failed", e);
        } finally {
            IN_APPENDER.remove();
        }
    }

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
        template.execute("CREATE INDEX IF NOT EXISTS idx_application_logs_created_at ON application_logs (created_at DESC)");
        SCHEMA_READY.set(true);
    }

    private String extractException(IThrowableProxy throwableProxy) {
        if (throwableProxy == null) {
            return null;
        }
        return ThrowableProxyUtil.asString(throwableProxy);
    }

    private String trim(String value) {
        if (value == null || value.length() <= MAX_VARCHAR) {
            return value;
        }
        return value.substring(0, MAX_VARCHAR);
    }

    private String trimText(String value) {
        if (value == null || value.length() <= MAX_TEXT) {
            return value;
        }
        return value.substring(0, MAX_TEXT);
    }

    private String mdcValue(ILoggingEvent eventObject, String key) {
        if (eventObject.getMDCPropertyMap() == null) {
            return null;
        }
        return eventObject.getMDCPropertyMap().get(key);
    }
}

