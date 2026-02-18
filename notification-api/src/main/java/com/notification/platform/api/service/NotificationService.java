package com.notification.platform.api.service;

import com.notification.platform.api.dto.NotificationRequest;
import com.notification.platform.api.dto.NotificationResponse;
import com.notification.platform.api.dto.NotificationStatusResponse;
import com.notification.platform.api.event.NotificationRequestedEvent;
import com.notification.platform.api.model.mongodb.DeliveryLogDocument;
import com.notification.platform.api.model.mongodb.IdempotencyDocument;
import com.notification.platform.api.model.mongodb.NotificationDocument;
import com.notification.platform.api.model.sql.Client;
import com.notification.platform.api.model.sql.Template;
import com.notification.platform.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String TOPIC_REQUESTED = "notifications.requested";
    private static final String STATUS_QUEUED = "QUEUED";

    private final NotificationMongoRepository notificationRepo;
    private final DeliveryLogMongoRepository deliveryLogRepo;
    private final IdempotencyMongoRepository idempotencyRepo;
    private final TemplateRepository templateRepo;
    private final ClientRepository clientRepo;
    private final KafkaTemplate<String, NotificationRequestedEvent> kafkaTemplate;

    public NotificationService(NotificationMongoRepository notificationRepo,
                              DeliveryLogMongoRepository deliveryLogRepo,
                              IdempotencyMongoRepository idempotencyRepo,
                              TemplateRepository templateRepo,
                              ClientRepository clientRepo,
                              KafkaTemplate<String, NotificationRequestedEvent> kafkaTemplate) {
        this.notificationRepo = notificationRepo;
        this.deliveryLogRepo = deliveryLogRepo;
        this.idempotencyRepo = idempotencyRepo;
        this.templateRepo = templateRepo;
        this.clientRepo = clientRepo;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        String clientId = request.clientId();
        String idempotencyKey = request.idempotencyKey();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyDocument> existing = idempotencyRepo.findByClientIdAndIdempotencyKey(clientId, idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent request: clientId={}, idempotencyKey={}, requestId={}",
                        clientId, idempotencyKey, existing.get().getRequestId());
                return new NotificationResponse(existing.get().getRequestId());
            }
        }

        validateRequest(request);

        String requestId = UUID.randomUUID().toString();

        NotificationDocument doc = new NotificationDocument(
                requestId,
                request.clientId(),
                request.channel(),
                request.to(),
                request.templateId(),
                request.variables() != null ? request.variables() : java.util.Map.of(),
                STATUS_QUEUED
        );
        notificationRepo.save(doc);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyDocument idempotency = new IdempotencyDocument(clientId, idempotencyKey, requestId);
            idempotencyRepo.save(idempotency);
        }

        NotificationRequestedEvent event = new NotificationRequestedEvent(
                requestId,
                request.clientId(),
                request.channel(),
                request.to(),
                request.templateId(),
                request.variables() != null ? request.variables() : java.util.Map.of(),
                1
        );
        kafkaTemplate.send(TOPIC_REQUESTED, requestId, event);

        log.info("Notification enqueued: requestId={}, clientId={}, channel={}", requestId, clientId, request.channel());
        return new NotificationResponse(requestId);
    }

    private void validateRequest(NotificationRequest request) {
        Template template = templateRepo.findByTemplateId(request.templateId())
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + request.templateId()));
        if (!template.getChannel().equalsIgnoreCase(request.channel())) {
            throw new IllegalArgumentException("Template channel does not match request channel");
        }

        Client client = clientRepo.findByClientId(request.clientId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + request.clientId()));
        if (client.getAllowedChannels() != null && !client.getAllowedChannels().contains(request.channel())) {
            throw new IllegalArgumentException("Client not allowed to use channel: " + request.channel());
        }
    }

    public Optional<NotificationStatusResponse> getNotificationStatus(String requestId) {
        return notificationRepo.findByRequestId(requestId)
                .map(doc -> {
                    List<DeliveryLogDocument> logs = deliveryLogRepo.findByRequestIdOrderByAttemptNoAsc(requestId);
                    List<NotificationStatusResponse.DeliveryLogSummary> summaries = logs.stream()
                            .map(l -> new NotificationStatusResponse.DeliveryLogSummary(
                                    l.getAttemptNo(),
                                    l.getProvider(),
                                    l.getStatus(),
                                    l.getLatencyMs(),
                                    l.getErrorCode(),
                                    l.getTimestamp()
                            ))
                            .toList();
                    return new NotificationStatusResponse(
                            doc.getRequestId(),
                            doc.getStatus(),
                            doc.getClientId(),
                            doc.getChannel(),
                            doc.getTo(),
                            doc.getTemplateId(),
                            doc.getCreatedAt(),
                            doc.getUpdatedAt(),
                            summaries
                    );
                });
    }
}
