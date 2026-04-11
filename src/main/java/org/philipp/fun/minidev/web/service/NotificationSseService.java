package org.philipp.fun.minidev.web.service;

import org.springframework.stereotype.Service;

@Service
public class NotificationSseService extends AbstractSseService {
    @Override
    public String getStreamId() {
        return "NOTIFICATIONS";
    }

    public void sendNotification(String message) {
        sendText(message, SseEventType.USER_MESSAGE, 0);
    }
}
