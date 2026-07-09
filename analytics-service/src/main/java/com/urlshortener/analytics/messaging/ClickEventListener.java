package com.urlshortener.analytics.messaging;

import com.urlshortener.analytics.service.ClickEventConsumer;
import com.urlshortener.common.dto.ClickEventMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClickEventListener {

    private static final Logger log = LoggerFactory.getLogger(ClickEventListener.class);

    private final ClickEventConsumer clickEventConsumer;

    @RabbitListener(queues = "click.events")
    public void handleClickEvent(ClickEventMessage message) {
        try {
            log.info("Received click event for shortCode={}", message.getShortCode());
            clickEventConsumer.process(message);
        } catch (Exception e) {
            log.error("Failed to process click event: {}", e.getMessage());
        }
    }
}
