package org.philipp.fun.minidev.web;

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
        AbstractSseService service = sseServices.get(streamId.toUpperCase());
        if (service != null) {
            return service.subscribe();
        }
        return null;
    }
}
