package com.notification.platform.worker.repository;

import com.notification.platform.worker.model.DeliveryLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeliveryLogMongoRepository extends MongoRepository<DeliveryLogDocument, String> {
}
