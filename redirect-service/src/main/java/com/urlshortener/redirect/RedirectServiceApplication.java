package com.urlshortener.redirect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(
    basePackages = {"com.urlshortener.redirect", "com.urlshortener.common"},
    excludeFilters = @ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
        classes = {
            com.urlshortener.common.security.JwtService.class,
            com.urlshortener.common.exception.GlobalExceptionHandler.class
        }
    )
)
public class RedirectServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedirectServiceApplication.class, args);
    }
}
