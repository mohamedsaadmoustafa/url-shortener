-- ==========================================
-- Flyway Migration V5
-- Description: Create abuse detection table
-- ==========================================

-- Ensure UUID generation function is available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE abuse_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    short_key VARCHAR(20) NOT NULL,
    ip_address inet NOT NULL,
    user_agent TEXT,
    referer TEXT,
    event_type VARCHAR(50) NOT NULL, -- e.g., CLICK, ATTEMPTED_SHORTEN
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_abuse_short_key ON abuse_events(short_key);
CREATE INDEX idx_abuse_ip_address ON abuse_events(ip_address);
CREATE INDEX idx_abuse_created_at ON abuse_events(created_at);
