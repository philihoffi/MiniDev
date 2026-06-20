package org.philipp.fun.minidev.controller;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.philipp.fun.minidev.sse.AbstractSseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller for SSE (Server-Sent Events) subscriptions.
 */
@RestController
@RequestMapping("/api/events")
public class SseController {

    /** Logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(SseController.class);

    /** Map of stream IDs to their respective SSE services. */
    private final Map<String, AbstractSseService> sseServices;

    /**
     * Constructs an SseController, building a service map from the provided list.
     *
     * @param services the list of SSE services
     */
    public SseController(List<AbstractSseService> services) {
        this.sseServices = services.stream()
                .collect(Collectors.toMap(
                        service -> service.getStreamId().toUpperCase(),
                        Function.identity()
                ));
    }

    /**
     * Subscribes to an SSE stream by its ID.
     *
     * @param streamId the stream identifier
     * @return an SseEmitter for the stream
     */
    @GetMapping(value = "/{streamId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String streamId) {
        LOG.info("Subscription request for stream: {}", streamId);
        AbstractSseService service = sseServices.get(streamId.toUpperCase());
        if (service != null) {
            return service.subscribe();
        }
        LOG.warn("Stream not found: {}", streamId);
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown stream: " + streamId);
    }
}
