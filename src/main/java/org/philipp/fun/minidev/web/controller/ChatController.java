package org.philipp.fun.minidev.web.controller;

import org.philipp.fun.minidev.core.AgentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/run/test")
    public String startTestRun() {
        return agentService.startNewRun().toString();
    }
}
