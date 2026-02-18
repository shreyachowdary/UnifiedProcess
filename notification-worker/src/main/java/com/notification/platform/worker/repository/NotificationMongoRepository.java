package com.notification.platform.worker.repository;

import com.notification.platform.worker.model.NotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NotificationMongoRepository extends MongoRepository<NotificationDocument, String> {

    Optional<NotificationDocument> findByRequestId(String requestId);
}
