-- V1 remains immutable. Align fresh V1 and legacy Hibernate schemas without changing rows.
-- PostgreSQL runs this migration in one transaction; fail quickly if the table is busy.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

ALTER TABLE songs
    ALTER COLUMN youtube_video_id TYPE VARCHAR(255),
    ALTER COLUMN anchor_points SET DEFAULT '[]'::jsonb,
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET DEFAULT NOW();
