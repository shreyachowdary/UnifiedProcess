package com.notification.platform.api.model.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "delivery_logs")
public class DeliveryLogDocument {

    @Id
    private String id;

    @Indexed
    private String requestId;
    private int attemptNo;
    private String provider;
    private String status;
    private Long latencyMs;
    private String errorCode;
    private Instant timestamp;

    public DeliveryLogDocument() {}

    public DeliveryLogDocument(String requestId, int attemptNo, String provider, String status,
                               Long latencyMs, String errorCode) {
        this.requestId = requestId;
        this.attemptNo = attemptNo;
        this.provider = provider;
        this.status = status;
        this.latencyMs = latencyMs;
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public int getAttemptNo() { return attemptNo; }
    public void setAttemptNo(int attemptNo) { this.attemptNo = attemptNo; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
