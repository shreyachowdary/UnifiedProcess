package com.notification.platform.api.dto;

import java.time.Instant;
import java.util.List;

public record NotificationStatusResponse(
        String requestId,
        String status,
        String clientId,
        String channel,
        String to,
        String templateId,
        Instant createdAt,
        Instant updatedAt,
        List<DeliveryLogSummary> deliveryLogs
) {
    public record DeliveryLogSummary(int attemptNo, String provider, String status, Long latencyMs, String errorCode, Instant timestamp) {}
}
