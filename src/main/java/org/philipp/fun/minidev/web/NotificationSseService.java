package org.philipp.fun.minidev.web;

import org.springframework.stereotype.Service;

@Service
public class NotificationSseService extends AbstractSseService {
    @Override
    public String getStreamId() {
        return "NOTIFICATIONS";
    }

    public void sendNotification(String message) {
        sendText(message, "UserMessage",0);
    }
}
