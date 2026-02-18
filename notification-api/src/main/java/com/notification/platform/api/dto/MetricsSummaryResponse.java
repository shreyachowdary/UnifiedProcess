package com.notification.platform.api.dto;

public record MetricsSummaryResponse(
        long total,
        double successRate,
        double avgLatencyMs,
        long lastHourVolume
) {}
