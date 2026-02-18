package com.notification.platform.worker.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.platform.worker.service.NotificationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class RetryConsumer {

    private static final Logger log = LoggerFactory.getLogger(RetryConsumer.class);

    private final NotificationProcessor processor;
    private final ObjectMapper objectMapper;

    public RetryConsumer(NotificationProcessor processor, ObjectMapper objectMapper) {
        this.processor = processor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topic.retry:notifications.retry}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(@Payload String message, Acknowledgment ack) {
        try {
            var event = objectMapper.readValue(message, NotificationConsumer.NotificationRequestedEvent.class);
            log.info("Retry notification: requestId={}, attempt={}", event.requestId(), event.attemptNo());
            processor.process(event);
            if (ack != null) {
                ack.acknowledge();
            }
        } catch (Exception e) {
            log.error("Retry processing failed: {}", e.getMessage());
            if (ack != null) {
                ack.acknowledge();
            }
            throw new RuntimeException(e);
        }
    }
}
