package com.autowizard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutoWizardApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutoWizardApplication.class, args);
    }
} 