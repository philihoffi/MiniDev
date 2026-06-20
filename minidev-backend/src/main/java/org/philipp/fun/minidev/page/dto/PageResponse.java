package org.philipp.fun.minidev.page.dto;

/**
 * DTO representing a page available for navigation.
 *
 * @param id             the page ID
 * @param path           the URL path
 * @param title          the display title
 * @param icon           the icon identifier
 * @param componentName  the frontend component name
 * @param roleRequired   the required role
 * @param navOrder       the navigation order
 */
public record PageResponse(
        long id,
        String path,
        String title,
        String icon,
        String componentName,
        String roleRequired,
        int navOrder
) {}