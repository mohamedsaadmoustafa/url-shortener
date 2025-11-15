-- ==========================================
-- Flyway Migration V3
-- Description: Function to add next month partition automatically
-- ==========================================

CREATE OR REPLACE FUNCTION create_next_month_partition()
RETURNS void AS $$
DECLARE
    next_month_start DATE;
    next_month_end DATE;
    partition_name TEXT;
BEGIN
    next_month_start := date_trunc('month', current_date) + interval '1 month';
    next_month_end := next_month_start + interval '1 month';
    partition_name := 'urls_' || to_char(next_month_start, 'YYYY_MM');

    IF NOT EXISTS (
        SELECT 1 FROM pg_tables
        WHERE schemaname = 'public' AND tablename = partition_name
    ) THEN
        EXECUTE format('
            CREATE TABLE %I PARTITION OF urls
            FOR VALUES FROM (%L) TO (%L);',
            partition_name,
            next_month_start::text,
            next_month_end::text
        );

        -- Indexes
        EXECUTE format('CREATE UNIQUE INDEX idx_%I_short_key ON %I(short_key);', partition_name, partition_name);
        EXECUTE format('CREATE INDEX idx_%I_created_at ON %I(created_at);', partition_name, partition_name);

        RAISE NOTICE 'Partition % created successfully.', partition_name;
    ELSE
        RAISE NOTICE 'Partition % already exists.', partition_name;
    END IF;
END;
$$ LANGUAGE plpgsql;
