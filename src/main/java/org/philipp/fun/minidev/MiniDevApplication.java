package org.philipp.fun.minidev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MiniDevApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniDevApplication.class, args);
    }

}
