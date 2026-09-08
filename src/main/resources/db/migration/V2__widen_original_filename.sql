DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'file_records'
          AND column_name = 'original_filename'
          AND data_type <> 'text'
    ) THEN
        ALTER TABLE file_records
            ALTER COLUMN original_filename TYPE TEXT;
    END IF;
END $$;
