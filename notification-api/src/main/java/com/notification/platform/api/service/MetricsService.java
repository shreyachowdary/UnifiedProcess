package com.notification.platform.api.service;

import com.notification.platform.api.dto.MetricsSummaryResponse;
import com.notification.platform.api.repository.NotificationMongoRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class MetricsService {

    private final NotificationMongoRepository notificationRepo;
    private final MongoTemplate mongoTemplate;

    public MetricsService(NotificationMongoRepository notificationRepo,
                          MongoTemplate mongoTemplate) {
        this.notificationRepo = notificationRepo;
        this.mongoTemplate = mongoTemplate;
    }

    public MetricsSummaryResponse getSummary() {
        long total = notificationRepo.count();
        long lastHourVolume = notificationRepo.countByCreatedAtAfter(Instant.now().minusSeconds(3600));

        List<String> successStatuses = List.of("SENT", "DELIVERED");
        long successCount = notificationRepo.countByStatusInAndCreatedAtAfter(successStatuses, Instant.EPOCH);

        double successRate = total > 0 ? (double) successCount / total * 100 : 0.0;

        Aggregation agg = newAggregation(
                match(Criteria.where("latencyMs").exists(true).ne(null)),
                group().avg("latencyMs").as("avgLatency")
        );
        AggregationResults<AvgResult> results = mongoTemplate.aggregate(agg, "delivery_logs", AvgResult.class);
        double avgLatencyMs = results.getUniqueMappedResult() != null
                ? results.getUniqueMappedResult().avgLatency
                : 0.0;

        return new MetricsSummaryResponse(total, successRate, avgLatencyMs, lastHourVolume);
    }

    private static class AvgResult {
        public double avgLatency;
    }
}
