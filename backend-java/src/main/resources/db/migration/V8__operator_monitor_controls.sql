ALTER TABLE monitor_controls ADD COLUMN tick_seconds int NOT NULL DEFAULT 5 CHECK(tick_seconds BETWEEN 5 AND 300);
ALTER TABLE monitor_controls ADD COLUMN disabled_rules jsonb NOT NULL DEFAULT '[]';
ALTER TABLE monitor_controls ADD COLUMN suppressed_until timestamptz;
ALTER TABLE monitor_controls ADD COLUMN next_tick_at timestamptz NOT NULL DEFAULT now();
