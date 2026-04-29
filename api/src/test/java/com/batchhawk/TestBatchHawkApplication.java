package com.batchhawk;

import org.springframework.boot.SpringApplication;

public class TestBatchHawkApplication {

    public static void main(String[] args) {
        SpringApplication.from(BatchHawkApplication::main)
            .with(TestcontainersConfig.class, LocalDevSecurityConfig.class)
            .run(args);
    }
}
