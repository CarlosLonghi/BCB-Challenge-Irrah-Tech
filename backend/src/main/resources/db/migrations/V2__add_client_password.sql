-- Hash BCrypt da senha. Aceita nulo: clientes cadastrados antes desta versao ficam sem senha e nao conseguem entrar
ALTER TABLE tb_clients ADD COLUMN password_hash VARCHAR(100);
