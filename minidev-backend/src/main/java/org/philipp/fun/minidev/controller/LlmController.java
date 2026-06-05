package org.philipp.fun.minidev.controller;

import org.philipp.fun.minidev.dto.llm.LlmModel;
import org.philipp.fun.minidev.service.LlmService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final LlmService llmService;

    public LlmController(LlmService llmService) {
        this.llmService = llmService;
    }

    @GetMapping("/models")
    public List<LlmModel> getModels(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "supported_parameters") String supportedParameters,
            @RequestParam(required = false, name = "output_modalities") String outputModalities
    ) {
        return llmService.getModels(category, supportedParameters, outputModalities);
    }
}
