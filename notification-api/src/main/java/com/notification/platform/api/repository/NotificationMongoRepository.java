package com.notification.platform.api.repository;

import com.notification.platform.api.model.mongodb.NotificationDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface NotificationMongoRepository extends MongoRepository<NotificationDocument, String> {

    Optional<NotificationDocument> findByRequestId(String requestId);

    @Query(value = "{ 'requestId': ?0 }", fields = "{ 'requestId': 1, 'status': 1, 'clientId': 1, 'channel': 1, 'to': 1, 'templateId': 1, 'createdAt': 1, 'updatedAt': 1 }")
    Optional<NotificationDocument> findByRequestIdProjected(String requestId);

    long countByClientIdAndCreatedAtBetween(String clientId, Instant start, Instant end);

    long countByStatus(String status);

    long countByStatusInAndCreatedAtAfter(java.util.List<String> statuses, Instant since);

    Page<NotificationDocument> findByClientIdOrderByCreatedAtDesc(String clientId, Pageable pageable);

    long countByCreatedAtAfter(Instant since);
}
