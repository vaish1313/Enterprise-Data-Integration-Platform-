package com.company.integrationplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IntegrationPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationPlatformApplication.class, args);
    }
}
