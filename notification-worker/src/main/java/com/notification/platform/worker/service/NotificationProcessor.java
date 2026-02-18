package com.notification.platform.worker.service;

import com.notification.platform.worker.consumer.NotificationConsumer.NotificationRequestedEvent;
import com.notification.platform.worker.model.TemplateEntity;
import com.notification.platform.worker.repository.TemplateJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessor.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final String TOPIC_RETRY = "notifications.retry";
    private static final String TOPIC_DLQ = "notifications.dlq";

    private final EmailProvider emailProvider;
    private final SmsProviderAdapter smsProvider;
    private final StatusUpdater statusUpdater;
    private final TemplateJpaRepository templateRepo;
    private final KafkaTemplate<String, NotificationRequestedEvent> kafkaTemplate;

    public NotificationProcessor(EmailProvider emailProvider,
                                 SmsProviderAdapter smsProvider,
                                 StatusUpdater statusUpdater,
                                 TemplateJpaRepository templateRepo,
                                 KafkaTemplate<String, NotificationRequestedEvent> kafkaTemplate) {
        this.emailProvider = emailProvider;
        this.smsProvider = smsProvider;
        this.statusUpdater = statusUpdater;
        this.templateRepo = templateRepo;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void process(NotificationRequestedEvent event) {
        String requestId = event.requestId();
        int attemptNo = event.attemptNo();

        statusUpdater.updateStatus(requestId, "PROCESSING", null, null, null);

        try {
            String body = resolveTemplate(event.templateId(), event.variables());
            String subject = resolveSubject(event.templateId());

            if ("EMAIL".equalsIgnoreCase(event.channel())) {
                emailProvider.send(event.to(), subject, body, requestId, attemptNo);
            } else if ("SMS".equalsIgnoreCase(event.channel())) {
                smsProvider.send(event.to(), body, requestId, attemptNo);
            } else {
                throw new IllegalArgumentException("Unknown channel: " + event.channel());
            }
        } catch (Exception e) {
            log.error("Notification failed: requestId={}, attempt={}, error={}", requestId, attemptNo, e.getMessage());
            // Delivery log already recorded by EmailProvider/SmsProviderAdapter on failure

            if (attemptNo < MAX_ATTEMPTS) {
                scheduleRetry(event);
            } else {
                sendToDlq(event);
            }
        }
    }

    private String resolveTemplate(String templateId, Map<String, String> variables) {
        return templateRepo.findByTemplateId(templateId)
                .map(TemplateEntity::getBody)
                .map(body -> applyVariables(body, variables))
                .orElse("No template found");
    }

    private String resolveSubject(String templateId) {
        return templateRepo.findByTemplateId(templateId)
                .map(TemplateEntity::getSubject)
                .orElse("");
    }

    private String applyVariables(String template, Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, String> e : variables.entrySet()) {
            result = result.replace("{{" + e.getKey() + "}}", e.getValue());
        }
        return result;
    }

    private void scheduleRetry(NotificationRequestedEvent event) {
        var retryEvent = new NotificationRequestedEvent(
                event.requestId(),
                event.clientId(),
                event.channel(),
                event.to(),
                event.templateId(),
                event.variables(),
                event.attemptNo() + 1
        );
        kafkaTemplate.send(TOPIC_RETRY, event.requestId(), retryEvent);
        statusUpdater.updateStatus(event.requestId(), "QUEUED", null, null, null);
        log.info("Scheduled retry: requestId={}, attempt={}", event.requestId(), event.attemptNo() + 1);
    }

    private void sendToDlq(NotificationRequestedEvent event) {
        kafkaTemplate.send(TOPIC_DLQ, event.requestId(), event);
        statusUpdater.updateStatus(event.requestId(), "FAILED", null, null, null);
        log.warn("Sent to DLQ: requestId={}", event.requestId());
    }
}
