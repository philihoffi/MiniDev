package org.philipp.fun.minidev.llm.controller;

import java.util.List;

import org.philipp.fun.minidev.llm.dto.LlmModel;
import org.philipp.fun.minidev.llm.service.LlmService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for LLM-related endpoints.
 */
@RestController
@RequestMapping("/api/llm")
public class LlmController {

    /** The LLM service. */
    private final LlmService llmService;

    /**
     * Constructs an LlmController.
     *
     * @param llmService the LLM service
     */
    public LlmController(LlmService llmService) {
        this.llmService = llmService;
    }

    /**
     * Returns available LLM models, optionally filtered.
     *
     * @param category            optional category filter
     * @param supportedParameters optional supported parameters filter
     * @param outputModalities    optional output modalities filter
     * @return list of matching models
     */
    @GetMapping("/models")
    public List<LlmModel> getModels(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "supported_parameters")
            String supportedParameters,
            @RequestParam(required = false, name = "output_modalities")
            String outputModalities) {
        return llmService.getModels(category, supportedParameters, outputModalities);
    }
}
