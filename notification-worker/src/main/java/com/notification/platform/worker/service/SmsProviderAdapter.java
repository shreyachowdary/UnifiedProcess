package com.notification.platform.worker.service;

import com.notification.platform.sms.SmsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsProviderAdapter {

    private static final Logger log = LoggerFactory.getLogger(SmsProviderAdapter.class);

    private final SmsProvider smsProvider;
    private final StatusUpdater statusUpdater;

    public SmsProviderAdapter(SmsProvider smsProvider, StatusUpdater statusUpdater) {
        this.smsProvider = smsProvider;
        this.statusUpdater = statusUpdater;
    }

    public void send(String to, String message, String requestId, int attemptNo) {
        SmsProvider.SendResult result = smsProvider.send(to, message);

        statusUpdater.recordDeliveryLog(requestId, attemptNo, "SMPP", result.status(), result.latencyMs(), result.errorCode());

        if ("SENT".equals(result.status())) {
            statusUpdater.updateStatus(requestId, "SENT", result.providerMessageId(), null, null);
            log.info("SMS sent: requestId={}, to={}, messageId={}", requestId, to, result.providerMessageId());
        } else {
            throw new RuntimeException("SMS send failed: " + result.errorCode());
        }
    }
}
