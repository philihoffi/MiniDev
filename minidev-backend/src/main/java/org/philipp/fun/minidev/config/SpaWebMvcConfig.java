package org.philipp.fun.minidev.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC configuration that forwards all non-file requests to {@code index.html}
 * for single-page application support.
 */
@Configuration
public class SpaWebMvcConfig implements WebMvcConfigurer {

    /**
     * Adds view controllers that forward unrecognised paths to the SPA entry
     * point.
     *
     * @param registry the view controller registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
        registry.addViewController("/**/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
    }
}