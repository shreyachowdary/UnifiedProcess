package com.notification.platform.worker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(EmailProvider.class);

    private final JavaMailSender mailSender;
    private final StatusUpdater statusUpdater;

    public EmailProvider(JavaMailSender mailSender, StatusUpdater statusUpdater) {
        this.mailSender = mailSender;
        this.statusUpdater = statusUpdater;
    }

    public void send(String to, String subject, String body, String requestId, int attemptNo) {
        long start = System.currentTimeMillis();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject != null ? subject : "");
            message.setText(body);
            mailSender.send(message);

            long latency = System.currentTimeMillis() - start;
            log.info("Email sent: requestId={}, to={}, latencyMs={}", requestId, to, latency);
            statusUpdater.recordDeliveryLog(requestId, attemptNo, "SMTP", "SENT", latency, null);
            statusUpdater.updateStatus(requestId, "SENT", "smtp-" + System.currentTimeMillis(), null, null);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            statusUpdater.recordDeliveryLog(requestId, attemptNo, "SMTP", "FAILED", latency, e.getMessage());
            throw new RuntimeException("Email send failed", e);
        }
    }
}
