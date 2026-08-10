package com.sepe.mvp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SepeMvpApplication {
    public static void main(String[] args) {
        SpringApplication.run(SepeMvpApplication.class, args);
    }
}
