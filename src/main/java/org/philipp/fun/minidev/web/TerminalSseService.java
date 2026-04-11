package org.philipp.fun.minidev.web;

import org.springframework.stereotype.Service;

@Service
public class TerminalSseService extends AbstractSseService {
    @Override
    public String getStreamId() {
        return "TERMINAL";
    }
    public void sendTerminalText(String text, String eventType, int delayMillis) {
        sendText(text, eventType, delayMillis);
    }

    public void clearTerminal() {
        sendClearCommand();
    }
}
