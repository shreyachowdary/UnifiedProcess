package com.notification.platform.worker.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.platform.worker.service.NotificationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationProcessor processor;
    private final ObjectMapper objectMapper;

    public NotificationConsumer(NotificationProcessor processor, ObjectMapper objectMapper) {
        this.processor = processor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topic.requested:notifications.requested}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_KEY) String key,
                        Acknowledgment ack) {
        try {
            var event = objectMapper.readValue(message, NotificationRequestedEvent.class);
            log.info("Consumed notification: requestId={}, channel={}", event.requestId(), event.channel());
            processor.process(event);
            if (ack != null) {
                ack.acknowledge();
            }
        } catch (Exception e) {
            log.error("Failed to process notification: key={}, error={}", key, e.getMessage());
            if (ack != null) {
                ack.acknowledge(); // Let retry/DLQ handle - or nack for retry
            }
            throw new RuntimeException(e);
        }
    }

    public record NotificationRequestedEvent(
            String requestId,
            String clientId,
            String channel,
            String to,
            String templateId,
            java.util.Map<String, String> variables,
            int attemptNo
    ) {}
}
