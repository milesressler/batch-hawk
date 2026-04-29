package com.batchhawk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BatchHawkApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchHawkApplication.class, args);
    }
}
