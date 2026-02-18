package com.notification.platform.worker.service;

import com.notification.platform.worker.model.DeliveryLogDocument;
import com.notification.platform.worker.model.NotificationDocument;
import com.notification.platform.worker.repository.DeliveryLogMongoRepository;
import com.notification.platform.worker.repository.NotificationMongoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StatusUpdater {

    private static final Logger log = LoggerFactory.getLogger(StatusUpdater.class);

    private final NotificationMongoRepository notificationRepo;
    private final DeliveryLogMongoRepository deliveryLogRepo;

    public StatusUpdater(NotificationMongoRepository notificationRepo,
                         DeliveryLogMongoRepository deliveryLogRepo) {
        this.notificationRepo = notificationRepo;
        this.deliveryLogRepo = deliveryLogRepo;
    }

    public void updateStatus(String requestId, String status, String providerMessageId, Long latencyMs, String errorCode) {
        notificationRepo.findByRequestId(requestId).ifPresent(doc -> {
            doc.setStatus(status);
            notificationRepo.save(doc);
            log.debug("Updated status: requestId={}, status={}", requestId, status);
        });
    }

    public void recordDeliveryLog(String requestId, int attemptNo, String provider, String status,
                                  Long latencyMs, String errorCode) {
        DeliveryLogDocument logDoc = new DeliveryLogDocument(requestId, attemptNo, provider, status, latencyMs, errorCode);
        deliveryLogRepo.save(logDoc);
    }
}
