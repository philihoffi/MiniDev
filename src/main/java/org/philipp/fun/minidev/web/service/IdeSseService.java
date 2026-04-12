package org.philipp.fun.minidev.web.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IdeSseService extends AbstractSseService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getStreamId() {
        return "IDE";
    }

    @Override
    protected boolean isHistoryEnabled() {
        return false;
    }

    public void sendFileUpdate(String fileType, String content) {
        Map<String, String> data = Map.of(
            "fileType", fileType,
            "content", content
        );
        broadcast(SseEventName.MESSAGE, data);
    }

    public void streamFileUpdate(String fileType, String oldContent, String newContent) {
        String oldC = oldContent != null ? oldContent : "";
        String newC = newContent != null ? newContent : "";

        // Switch Tab first
        switchTab(fileType);

        // Find common prefix
        int commonPrefixLength = 0;
        int minLength = Math.min(oldC.length(), newC.length());
        while (commonPrefixLength < minLength && oldC.charAt(commonPrefixLength) == newC.charAt(commonPrefixLength)) {
            commonPrefixLength++;
        }

        // 1. Erase phase
        for (int i = oldC.length(); i > commonPrefixLength; i--) {
            sendDelete(fileType);
            sleep(1);
        }

        // 2. Write phase
        for (int i = commonPrefixLength; i < newC.length(); i++) {
            sendAppend(fileType, String.valueOf(newC.charAt(i)));
            sleep(2);
        }
    }

    private void switchTab(String fileType) {
        try {
            broadcast(SseEventName.EVENT, SseEventType.SWITCH_TAB, fileType);
        } catch (Exception e) {
            log.error("Failed to send switch tab event", e);
        }
    }

    private void sendAppend(String fileType, String character) {
        Map<String, String> data = Map.of(
            "fileType", fileType,
            "char", character
        );
        broadcast(SseEventName.EVENT, SseEventType.FILE_APPEND, data);
    }

    private void sendDelete(String fileType) {
        try {
            broadcast(SseEventName.EVENT, SseEventType.FILE_DELETE, fileType);
        } catch (Exception e) {
            log.error("Failed to send file delete", e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
