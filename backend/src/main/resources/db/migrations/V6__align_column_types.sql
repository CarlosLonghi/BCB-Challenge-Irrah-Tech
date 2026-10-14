-- Datas do cliente com fuso, como as das conversas e mensagens. Os valores antigos foram gravados
-- pelo Hibernate como LocalDateTime no fuso da JVM (UTC no Docker) e sao lidos como UTC
ALTER TABLE tb_clients
    ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE USING updated_at AT TIME ZONE 'UTC';

-- Id do destinatario com o mesmo tipo dos demais ids
ALTER TABLE tb_conversations ALTER COLUMN recipient_id TYPE BIGINT;
ALTER TABLE tb_messages ALTER COLUMN recipient_id TYPE BIGINT;
