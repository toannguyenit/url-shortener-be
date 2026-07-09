package com.urlshortener.url;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.urlshortener.url", "com.urlshortener.common"})
@EnableMongoAuditing
@EnableScheduling
public class UrlServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlServiceApplication.class, args);
    }
}
