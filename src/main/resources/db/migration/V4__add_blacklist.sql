-- ==========================================
-- Flyway Migration V4
-- Description: Create blacklist_urls table
-- ==========================================

CREATE TABLE IF NOT EXISTS blacklist_urls (
    id BIGSERIAL PRIMARY KEY,
    url_pattern VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Index for faster pattern searches
CREATE INDEX IF NOT EXISTS idx_blacklist_url_pattern ON blacklist_urls(url_pattern);
