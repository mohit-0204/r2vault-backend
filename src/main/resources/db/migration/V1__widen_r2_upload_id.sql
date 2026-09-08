DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'upload_sessions'
          AND column_name = 'upload_id'
          AND data_type <> 'text'
    ) THEN
        ALTER TABLE upload_sessions
            ALTER COLUMN upload_id TYPE TEXT;
    END IF;
END $$;
