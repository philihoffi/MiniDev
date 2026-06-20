package org.philipp.fun.minidev.page.service;

import java.util.List;
import java.util.stream.Collectors;

import org.philipp.fun.minidev.page.dto.PageResponse;
import org.philipp.fun.minidev.page.repository.PageRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for page-related operations.
 */
@Service
@Transactional(readOnly = true)
public class PageService {

    /** Repository for page data. */
    private final PageRepository pageRepository;

    /**
     * Constructs a PageService.
     *
     * @param pageRepository the page repository
     */
    public PageService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    /**
     * Returns the pages available for the given authentication.
     *
     * @param authentication the current authentication
     * @return list of available page responses
     */
    public List<PageResponse> getAvailablePages(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return pageRepository.findByEnabledTrueAndRoleRequiredIsNullOrRoleRequiredOrderByNavOrderAsc("GUEST")
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }

        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .map(a -> a.substring(5))
                .orElse("USER");

        return pageRepository.findByEnabledTrueAndRoleRequiredIsNullOrRoleRequiredOrderByNavOrderAsc(role)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Converts a Page entity to a PageResponse DTO.
     *
     * @param page the page entity
     * @return the page response
     */
    private PageResponse toResponse(org.philipp.fun.minidev.page.model.Page page) {
        return new PageResponse(page.getId(), page.getPath(), page.getTitle(),
                page.getIcon(), page.getComponentName(), page.getRoleRequired(), page.getNavOrder());
    }
}