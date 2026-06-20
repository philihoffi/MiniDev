package org.philipp.fun.minidev.page.controller;

import java.util.List;

import org.philipp.fun.minidev.page.dto.PageResponse;
import org.philipp.fun.minidev.page.service.PageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for page-related operations.
 */
@RestController
@RequestMapping("/api/pages")
public class PageController {

    /** Service for page operations. */
    private final PageService pageService;

    /**
     * Constructs a PageController.
     *
     * @param pageService the page service
     */
    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    /**
     * Returns available pages for the authenticated user.
     *
     * @param authentication the current authentication
     * @return list of available pages
     */
    @GetMapping
    public ResponseEntity<List<PageResponse>> getPages(Authentication authentication) {
        return ResponseEntity.ok(pageService.getAvailablePages(authentication));
    }
}