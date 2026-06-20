package org.philipp.fun.minidev;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Servlet initializer that configures the Spring Boot application for
 * deployment in a traditional servlet container.
 */
public class ServletInitializer extends SpringBootServletInitializer {

    /**
     * Configures the application source class.
     *
     * @param application the Spring application builder
     * @return the configured builder
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MiniDevApplication.class);
    }
}