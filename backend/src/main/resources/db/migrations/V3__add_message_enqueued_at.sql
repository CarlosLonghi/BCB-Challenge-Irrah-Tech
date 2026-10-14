-- Momento em que a mensagem foi publicada no RabbitMQ. Nulo = commit feito, mas a publicacao falhou
ALTER TABLE tb_messages ADD COLUMN enqueued_at TIMESTAMP(6) WITH TIME ZONE;

-- Mensagens anteriores contam como publicadas; as que ficaram pela metade a recuperacao da inicializacao ja cobre
UPDATE tb_messages SET enqueued_at = created_at;

-- So as mensagens nunca publicadas, que a rotina periodica procura
CREATE INDEX ix_messages_unpublished ON tb_messages (created_at) WHERE status = 'QUEUED' AND enqueued_at IS NULL;
