package com.notification.platform.api.repository;

import com.notification.platform.api.model.mongodb.IdempotencyDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IdempotencyMongoRepository extends MongoRepository<IdempotencyDocument, String> {


    boolean existsByClientIdAndIdempotencyKey(String clientId, String idempotencyKey);

    Optional<IdempotencyDocument> findByClientIdAndIdempotencyKey(String clientId, String idempotencyKey);
}
