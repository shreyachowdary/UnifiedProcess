package com.notification.platform.api.model.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "notifications")
@CompoundIndex(name = "clientId_createdAt", def = "{'clientId': 1, 'createdAt': -1}")
@CompoundIndex(name = "status_createdAt", def = "{'status': 1, 'createdAt': -1}")
public class NotificationDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String requestId;

    private String clientId;
    private String channel;
    private String to;
    private String templateId;
    private Map<String, String> variables;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public NotificationDocument() {}

    public NotificationDocument(String requestId, String clientId, String channel, String to,
                                String templateId, Map<String, String> variables, String status) {
        this.requestId = requestId;
        this.clientId = clientId;
        this.channel = channel;
        this.to = to;
        this.templateId = templateId;
        this.variables = variables;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }
    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
