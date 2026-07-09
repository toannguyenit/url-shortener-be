package com.urlshortener.analytics.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitQueueConfig {

    public static final String EXCHANGE = "url.events";
    public static final String CLICK_QUEUE = "click.events";
    public static final String CLICK_ROUTING_KEY = "click";

    @Bean
    public TopicExchange urlEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue clickEventsQueue() {
        return new Queue(CLICK_QUEUE, true);
    }

    @Bean
    public Binding clickEventsBinding(Queue clickEventsQueue, TopicExchange urlEventsExchange) {
        return BindingBuilder.bind(clickEventsQueue).to(urlEventsExchange).with(CLICK_ROUTING_KEY);
    }
}
