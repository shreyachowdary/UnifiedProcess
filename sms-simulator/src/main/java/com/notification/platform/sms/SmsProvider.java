package com.notification.platform.sms;

/**
 * Interface for SMS delivery. Implement with real SMSC (e.g. jSMPP/OpenSMPP) or mock simulator.
 * Swapping to real SMSC: implement this interface with your SMPP client and inject as bean.
 */
public interface SmsProvider {

    /**
     * Send SMS to the given destination.
     *
     * @param to      recipient phone number (E.164 format recommended)
     * @param message SMS body text
     * @return SendResult with providerMessageId and status
     */
    SendResult send(String to, String message);

    record SendResult(String providerMessageId, String status, long latencyMs, String errorCode) {
        public static SendResult success(String messageId, long latencyMs) {
            return new SendResult(messageId, "SENT", latencyMs, null);
        }

        public static SendResult failure(String errorCode, long latencyMs) {
            return new SendResult(null, "FAILED", latencyMs, errorCode);
        }
    }
}
