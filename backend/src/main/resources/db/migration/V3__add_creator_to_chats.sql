ALTER TABLE chats ADD COLUMN IF NOT EXISTS creator_id bigint;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'FK_chat_creator'
    ) THEN
        ALTER TABLE chats ADD CONSTRAINT FK_chat_creator FOREIGN KEY (creator_id) REFERENCES users(id);
    END IF;
END $$;
