package com.notification.platform.api;

import com.notification.platform.api.dto.NotificationRequest;
import com.notification.platform.api.dto.NotificationResponse;
import com.notification.platform.api.event.NotificationRequestedEvent;
import com.notification.platform.api.model.mongodb.NotificationDocument;
import com.notification.platform.api.model.sql.Client;
import com.notification.platform.api.model.sql.Template;
import com.notification.platform.api.repository.*;
import com.notification.platform.api.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationMongoRepository notificationRepo;
    @Mock private DeliveryLogMongoRepository deliveryLogRepo;
    @Mock private IdempotencyMongoRepository idempotencyRepo;
    @Mock private TemplateRepository templateRepo;
    @Mock private ClientRepository clientRepo;
    @Mock private KafkaTemplate<String, NotificationRequestedEvent> kafkaTemplate;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepo, deliveryLogRepo, idempotencyRepo,
                templateRepo, clientRepo, kafkaTemplate
        );
    }

    @Test
    void createNotification_shouldReturn202AndPublishToKafka() {
        NotificationRequest request = new NotificationRequest(
                "client1", "EMAIL", "user@example.com", "welcome-email",
                Map.of("name", "John"), null
        );

        Template template = new Template();
        template.setTemplateId("welcome-email");
        template.setChannel("EMAIL");
        when(templateRepo.findByTemplateId("welcome-email")).thenReturn(Optional.of(template));

        Client client = new Client();
        client.setClientId("client1");
        client.setAllowedChannels(List.of("EMAIL", "SMS"));
        when(clientRepo.findByClientId("client1")).thenReturn(Optional.of(client));

        when(idempotencyRepo.findByClientIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());

        NotificationResponse response = notificationService.createNotification(request);

        assertNotNull(response.requestId());
        verify(notificationRepo).save(any(NotificationDocument.class));
        verify(kafkaTemplate).send(eq("notifications.requested"), eq(response.requestId()), any(NotificationRequestedEvent.class));
    }

    @Test
    void createNotification_withIdempotencyKey_shouldReturnExistingRequestId() {
        NotificationRequest request = new NotificationRequest(
                "client1", "EMAIL", "user@example.com", "welcome-email",
                Map.of(), "key-123"
        );

        var idempotency = new com.notification.platform.api.model.mongodb.IdempotencyDocument("client1", "key-123", "existing-request-id");
        when(idempotencyRepo.findByClientIdAndIdempotencyKey("client1", "key-123"))
                .thenReturn(Optional.of(idempotency));

        NotificationResponse response = notificationService.createNotification(request);

        assertEquals("existing-request-id", response.requestId());
        verify(notificationRepo, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
