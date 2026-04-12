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
        String formatted = formatCode(fileType, content);
        Map<String, String> data = Map.of(
            "fileType", fileType,
            "content", formatted
        );
        broadcast(SseEventName.MESSAGE, data);
    }

    public void streamFileUpdate(String fileType, String oldContent, String newContent) {
        String oldC = oldContent != null ? oldContent : "";
        String newC = formatCode(fileType, newContent != null ? newContent : "");

        // Switch Tab first
        switchTab(fileType);

        // Find common prefix
        int commonPrefixLength = 0;
        int minOld = oldC.length();
        int minNew = newC.length();
        int minLength = Math.min(minOld, minNew);
        while (commonPrefixLength < minLength && oldC.charAt(commonPrefixLength) == newC.charAt(commonPrefixLength)) {
            commonPrefixLength++;
        }

        // Find common suffix (to avoid rewriting the whole thing)
        int commonSuffixLength = 0;
        int maxSuffix = Math.min(minOld - commonPrefixLength, minNew - commonPrefixLength);
        while (commonSuffixLength < maxSuffix && oldC.charAt(minOld - 1 - commonSuffixLength) == newC.charAt(minNew - 1 - commonSuffixLength)) {
            commonSuffixLength++;
        }

        // 1. Erase phase (up to the point where suffix starts)
        int deleteCount = minOld - commonPrefixLength - commonSuffixLength;
        for (int i = 0; i < deleteCount; i++) {
            // Delete character at the point where new content will be inserted
            sendDelete(fileType, commonPrefixLength);
            sleep(1);
        }

        // 2. Write phase (the new part between prefix and suffix)
        int writeEnd = minNew - commonSuffixLength;
        for (int i = commonPrefixLength; i < writeEnd; i++) {
            sendAppend(fileType, String.valueOf(newC.charAt(i)), i);
            sleep(2);
        }
    }

    private String formatCode(String fileType, String content) {
        if (content == null || content.isBlank()) return "";
        
        if ("html".equalsIgnoreCase(fileType)) {
            try {
                org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(content);
                doc.outputSettings()
                        .indentAmount(4)
                        .prettyPrint(true)
                        .outline(true);
                return doc.outerHtml();
            } catch (Exception e) {
                log.warn("Failed to format HTML code: {}", e.getMessage());
                return content;
            }
        }
        // For CSS and JS we could add more formatters if needed.
        // For now, let's at least ensure consistent line endings and trim.
        return content.trim();
    }

    private void switchTab(String fileType) {
        try {
            broadcast(SseEventName.EVENT, SseEventType.SWITCH_TAB, fileType);
        } catch (Exception e) {
            log.error("Failed to send switch tab event", e);
        }
    }

    private void sendAppend(String fileType, String character, int position) {
        Map<String, Object> data = Map.of(
            "fileType", fileType,
            "char", character,
            "position", position
        );
        broadcast(SseEventName.EVENT, SseEventType.FILE_APPEND, data);
    }

    private void sendDelete(String fileType, int position) {
        try {
            Map<String, Object> data = Map.of(
                "fileType", fileType,
                "position", position
            );
            broadcast(SseEventName.EVENT, SseEventType.FILE_DELETE, data);
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
