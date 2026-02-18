package com.notification.platform.api.model.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "idempotency_keys")
@CompoundIndex(name = "clientId_idempotencyKey", def = "{'clientId': 1, 'idempotencyKey': 1}", unique = true)
public class IdempotencyDocument {

    @Id
    private String id;
    private String clientId;
    private String idempotencyKey;
    private String requestId;
    private Instant createdAt;

    public IdempotencyDocument() {}

    public IdempotencyDocument(String clientId, String idempotencyKey, String requestId) {
        this.clientId = clientId;
        this.idempotencyKey = idempotencyKey;
        this.requestId = requestId;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
