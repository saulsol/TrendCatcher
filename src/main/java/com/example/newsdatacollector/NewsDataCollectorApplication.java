package com.example.newsdatacollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NewsDataCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsDataCollectorApplication.class, args);
    }

}
