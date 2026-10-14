-- Indices para as listas paginadas e as FKs (o Postgres nao indexa FK sozinho)

-- GET /conversations: conversas do cliente, mais recente primeiro
CREATE INDEX ix_conversations_client_last_message ON tb_conversations (client_id, last_message_time DESC, id DESC);

-- GET /conversations/{id}/messages com cursor por id
CREATE INDEX ix_messages_conversation_id ON tb_messages (conversation_id, id);

-- GET /messages com cursor por id
CREATE INDEX ix_messages_sender_id ON tb_messages (sender_id, id);

-- GET /queue/status e recuperacao por status
CREATE INDEX ix_messages_status_priority ON tb_messages (status, priority);

-- Extrato passa a paginar por id
DROP INDEX ix_ledger_entries_client;
CREATE INDEX ix_ledger_entries_client_id ON tb_ledger_entries (client_id, id DESC);
