package com.notification.platform.api.repository;

import com.notification.platform.api.model.mongodb.DeliveryLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DeliveryLogMongoRepository extends MongoRepository<DeliveryLogDocument, String> {

    List<DeliveryLogDocument> findByRequestIdOrderByAttemptNoAsc(String requestId);
}
