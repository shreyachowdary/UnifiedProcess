package com.notification.platform.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock SMSC simulator that emulates SMPP responses and delivery receipts.
 * Replace with real jSMPP/OpenSMPP implementation for production SMSC integration.
 */
public class MockSmppSimulator implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(MockSmppSimulator.class);

    private static final double FAILURE_RATE = 0.05; // 5% simulated failure for testing
    private static final long MIN_LATENCY_MS = 50;
    private static final long MAX_LATENCY_MS = 200;

    @Override
    public SmsProvider.SendResult send(String to, String message) {
        long start = System.currentTimeMillis();
        simulateNetworkLatency();

        if (ThreadLocalRandom.current().nextDouble() < FAILURE_RATE) {
            log.warn("Simulated SMS failure: to={}", to);
            return SmsProvider.SendResult.failure("SIMULATED_FAILURE", System.currentTimeMillis() - start);
        }

        String messageId = "mock-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Simulated SMS sent: to={}, messageId={}", to, messageId);
        return SmsProvider.SendResult.success(messageId, System.currentTimeMillis() - start);
    }

    private void simulateNetworkLatency() {
        try {
            long delay = ThreadLocalRandom.current().nextLong(MIN_LATENCY_MS, MAX_LATENCY_MS);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during simulated latency", e);
        }
    }
}
