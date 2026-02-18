-- Unified Notification Platform - PostgreSQL Schema
-- Run this against notification_db before starting the API/Worker

-- Templates: email/sms templates with variable placeholders
CREATE TABLE IF NOT EXISTS templates (
    id BIGSERIAL PRIMARY KEY,
    template_id VARCHAR(255) NOT NULL UNIQUE,
    channel VARCHAR(50) NOT NULL,
    subject VARCHAR(500),
    body TEXT NOT NULL,
    variables JSONB,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_template_id ON templates(template_id);

-- Clients: API clients with quotas and allowed channels
CREATE TABLE IF NOT EXISTS clients (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    daily_quota INTEGER,
    allowed_channels JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_client_id ON clients(client_id);

-- Routing rules: channel-to-provider mapping per client
CREATE TABLE IF NOT EXISTS routing_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(255) NOT NULL UNIQUE,
    client_id VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    priority INTEGER DEFAULT 0,
    enabled BOOLEAN DEFAULT true
);
CREATE INDEX IF NOT EXISTS idx_routing_client_channel ON routing_rules(client_id, channel);

-- Seed data for local development
INSERT INTO templates (template_id, channel, subject, body, variables) VALUES
    ('welcome-email', 'EMAIL', 'Welcome!', 'Hello {{name}}, welcome to our platform!', '["name"]'),
    ('welcome-sms', 'SMS', NULL, 'Hi {{name}}, welcome!', '["name"]'),
    ('otp-sms', 'SMS', NULL, 'Your OTP is {{code}}. Valid for 5 minutes.', '["code"]')
ON CONFLICT (template_id) DO NOTHING;

INSERT INTO clients (client_id, name, daily_quota, allowed_channels) VALUES
    ('client1', 'Demo Client', 1000, '["EMAIL", "SMS"]')
ON CONFLICT (client_id) DO NOTHING;

INSERT INTO routing_rules (rule_id, client_id, channel, provider, priority, enabled) VALUES
    ('rule-email-1', 'client1', 'EMAIL', 'SMTP', 1, true),
    ('rule-sms-1', 'client1', 'SMS', 'SMPP', 1, true)
ON CONFLICT (rule_id) DO NOTHING;
