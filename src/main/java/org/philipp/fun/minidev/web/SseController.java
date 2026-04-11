package org.philipp.fun.minidev.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events")
public class SseController {

    private final SseService sseService;

    public SseController(SseService sseService) {
        this.sseService = sseService;
    }

    @GetMapping(value = "/{streamId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String streamId) {
        try {
            SseService.StreamType topic = SseService.StreamType.valueOf(streamId.toUpperCase());
            return sseService.subscribe(topic);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
