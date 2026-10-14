-- Momento da entrega; a leitura simulada (ReadSimulator) marca READ algum tempo depois
ALTER TABLE tb_messages ADD COLUMN delivered_at TIMESTAMP(6) WITH TIME ZONE;

-- Entregues antes desta versao: contam como entregues na criacao (viram READ na primeira rodada)
UPDATE tb_messages SET delivered_at = created_at WHERE status = 'DELIVERED';

CREATE INDEX ix_messages_delivered ON tb_messages (delivered_at) WHERE status = 'DELIVERED';
