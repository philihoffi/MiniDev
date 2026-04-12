package org.philipp.fun.minidev.web.controller;

import org.philipp.fun.minidev.core.GameStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
public class PageController {

    private final String applicationName;
    private final GameStorageService gameStorageService;

    public PageController(@Value("${spring.application.name}") String applicationName, GameStorageService gameStorageService) {
        this.applicationName = applicationName;
        this.gameStorageService = gameStorageService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appName", applicationName);
        model.addAttribute("viewTitle", "Initial View");
        model.addAttribute("tagline", "Tiny autonomous game developer");
        return "index";
    }

    @GetMapping("/ide")
    public String ide(Model model) {
        model.addAttribute("appName", applicationName);
        return "ide";
    }

    @GetMapping("/games-static/run-{runId}")
    public ResponseEntity<String> playProject(@PathVariable UUID runId) {
        String content = gameStorageService.getGameContent(runId);
        if (content != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
        }
        return ResponseEntity.notFound().build();
    }

}
