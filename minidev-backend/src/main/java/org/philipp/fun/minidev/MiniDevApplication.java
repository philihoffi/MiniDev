package org.philipp.fun.minidev;

import java.util.concurrent.Executor;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Entry point for the MiniDev application.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties
public class MiniDevApplication {

    /**
     * Main method that starts the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MiniDevApplication.class, args);
    }

    /**
     * Provides a shared {@link ObjectMapper} bean.
     *
     * @return a new ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Provides a global thread pool executor bean.
     *
     * @return a configured ThreadPoolTaskExecutor
     */
    @Bean(name = "globalExecutor")
    public Executor globalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 4);
        executor.setThreadNamePrefix("global-executor-");
        executor.initialize();

        return executor;
    }

}
