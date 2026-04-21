package org.philipp.fun.minidev.controller;

import org.philipp.fun.minidev.services.AbstractSseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
public class SseController {

    private static final Logger log = LoggerFactory.getLogger(SseController.class);
    private final Map<String, AbstractSseService> sseServices;

    public SseController(List<AbstractSseService> services) {
        this.sseServices = services.stream()
                .collect(Collectors.toMap(
                        service -> service.getStreamId().toUpperCase(),
                        Function.identity()
                ));
    }

    @GetMapping(value = "/{streamId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String streamId) {
        log.info("Subscription request for stream: {}", streamId);
        AbstractSseService service = sseServices.get(streamId.toUpperCase());
        if (service != null) {
            return service.subscribe();
        }
        log.warn("Stream not found: {}", streamId);
        return null;
    }
}
