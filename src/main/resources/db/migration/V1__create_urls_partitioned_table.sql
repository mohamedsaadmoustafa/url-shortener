-- ==========================================
-- Flyway Migration V1
-- Description: Create partitioned urls table
-- ==========================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE urls (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    short_key VARCHAR(20) NOT NULL,
    original_url TEXT NOT NULL,
    custom_alias BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    click_count BIGINT DEFAULT 0,
    deleted_at TIMESTAMP,
    PRIMARY KEY (id, created_at),
    UNIQUE (short_key, created_at)
) PARTITION BY RANGE (created_at);
