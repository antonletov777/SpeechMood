ALTER TABLE chats ADD COLUMN IF NOT EXISTS creator_id bigint;
ALTER TABLE chats ADD CONSTRAINT FK_chat_creator FOREIGN KEY (creator_id) REFERENCES users(id);
