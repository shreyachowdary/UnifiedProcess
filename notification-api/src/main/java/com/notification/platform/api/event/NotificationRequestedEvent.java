package com.notification.platform.api.event;

import java.util.Map;

public record NotificationRequestedEvent(
        String requestId,
        String clientId,
        String channel,
        String to,
        String templateId,
        Map<String, String> variables,
        int attemptNo
) {}
