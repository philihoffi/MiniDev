package org.philipp.fun.minidev.web.service;

import org.springframework.stereotype.Service;

@Service
public class TerminalSseService extends AbstractSseService {
    @Override
    public String getStreamId() {
        return "TERMINAL";
    }

    @Override
    protected boolean isHistoryEnabled() {
        return true;
    }

    public void sendTerminalText(String text, SseEventType eventType) {
        sendText(text, eventType);
    }

    public void clearTerminal() {
        sendClearCommand();
    }
}
