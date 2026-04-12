package org.philipp.fun.minidev.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final String applicationName;

    public PageController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
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

}
