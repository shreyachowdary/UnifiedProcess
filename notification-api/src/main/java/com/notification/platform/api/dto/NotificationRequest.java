package com.notification.platform.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public record NotificationRequest(
        @NotBlank(message = "clientId is required")
        String clientId,

        @NotNull(message = "channel is required")
        @Pattern(regexp = "^(EMAIL|SMS)$", message = "channel must be EMAIL or SMS")
        String channel,

        @NotBlank(message = "to is required")
        String to,

        @NotBlank(message = "templateId is required")
        String templateId,

        Map<String, String> variables,

        String idempotencyKey
) {}
