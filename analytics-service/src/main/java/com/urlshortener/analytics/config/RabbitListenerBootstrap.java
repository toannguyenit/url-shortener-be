package com.urlshortener.analytics.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitListenerBootstrap {

    private final RabbitListenerEndpointRegistry registry;

    @EventListener(ApplicationReadyEvent.class)
    public void startListeners() {
        try {
            if (!registry.isRunning()) {
                registry.start();
            }
            log.info("RabbitMQ click event listener started");
        } catch (Exception e) {
            log.error(
                    "RabbitMQ listener failed to start ({}). Analytics API remains available; click events will not be consumed until RabbitMQ is fixed.",
                    e.getMessage()
            );
        }
    }
}
