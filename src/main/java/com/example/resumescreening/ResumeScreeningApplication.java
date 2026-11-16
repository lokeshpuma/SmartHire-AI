package com.example.resumescreening;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ResumeScreeningApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeScreeningApplication.class, args);
    }
}
