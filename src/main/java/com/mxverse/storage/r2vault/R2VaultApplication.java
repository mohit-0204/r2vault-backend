package com.mxverse.storage.r2vault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class R2VaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(R2VaultApplication.class, args);
    }

}
