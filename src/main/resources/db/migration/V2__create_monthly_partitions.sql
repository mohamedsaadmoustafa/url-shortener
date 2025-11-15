-- ==========================================
-- Flyway Migration V2
-- Description: Create initial monthly partitions for 2025-2026
-- ==========================================

DO $$
DECLARE
    start_month DATE := '2025-11-01';
    end_month DATE;
    partition_name TEXT;
BEGIN
    FOR i IN 0..11 LOOP
        end_month := start_month + interval '1 month';
        partition_name := 'urls_' || to_char(start_month, 'YYYY_MM');

        EXECUTE format('
            CREATE TABLE %I PARTITION OF urls
            FOR VALUES FROM (%L) TO (%L);',
            partition_name,
            start_month::text,
            end_month::text
        );

        -- Indexes
        EXECUTE format('CREATE UNIQUE INDEX idx_%I_short_key ON %I(short_key);', partition_name, partition_name);
        EXECUTE format('CREATE INDEX idx_%I_created_at ON %I(created_at);', partition_name, partition_name);

        start_month := end_month;
    END LOOP;
END $$;
