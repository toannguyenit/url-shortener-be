package com.urlshortener.analytics.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitQueueConfig {

    @Bean
    public Queue clickEventsQueue() {
        return new Queue("click.events", true);
    }
}
