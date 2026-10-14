# Estornar mensagens FAILED, registrar extrato e reenfileirar publicações perdidas

- Status: concluído
- Criado em: 2026-10-14 · Branch: `feat/fila-estorno-extrato` (a partir de `feat/auth-senha-jwt`)

## Objetivo e contexto
Fase 2 do roadmap (confiabilidade da fila e do dinheiro). Hoje:
- uma mensagem que vira `FAILED` continua cobrada;
- o saldo/limite é só um número sobrescrito, sem histórico;
- se a publicação no RabbitMQ falha no `afterCommit`, a mensagem fica `QUEUED` no banco até o próximo restart do backend.

Resultado esperado: estorno automático e único no `FAILED`, extrato por cliente (`GET /clients/{id}/ledger`) e uma rotina periódica que republica as mensagens nunca publicadas.

## Estado atual
- `MessageService.sendMessage` trava o cliente (`findWithLockById`), debita com `ClientService.debit`, grava a `Message` e publica depois do commit (`enqueueAfterCommit`). Se a publicação falhar, só loga o erro.
- `RabbitConfig.messageRecoverer` → `MessageProcessor.markFailed` → `updateStatus(FAILED)`. Não há estorno.
- `PendingMessagesRecovery.requeuePendingMessages` roda só no `ApplicationReadyEvent`.
- Invariantes (ARCHITECTURE.md): o status só muda via update de coluna, e a publicação acontece só depois do commit.

## Plano de trabalho
### Marco 1: extrato (`tb_ledger_entries`)
- Migration `V4__create_ledger_entries.sql`: tabela com `client_id`, `message_id`, `type` (`DEBIT`/`REFUND`), `amount` > 0 e `created_at`. Backfill com um `DEBIT` por mensagem já existente.
- Entidade `LedgerEntry`, enum `LedgerEntryType`, `LedgerEntryRepository` e `LedgerService` (`record`, `findByClient`). `MessageService.sendMessage` registra o `DEBIT` na mesma transação.
- `GET /clients/{id}/ledger` (só o próprio cliente; outro → 403, como no `/balance`).
- Aceite: `ClientIT`/`MessageFlowIT` mostram o `DEBIT` no extrato.

### Marco 2: estorno no FAILED
- `MessageRepository.updateStatusIfIn(id, from, to)`: update condicional que devolve as linhas alteradas.
- `MessageService.failAndRefund(messageId)` (@Transactional): trava o cliente, tira a mensagem de pendente para `FAILED` e, só se essa transição aconteceu, devolve o `cost` (pré-pago → `balance`, pós-pago → `limit`) com `ClientService.refund` e registra o `REFUND`.
- O `MessageRecoverer` chama `failAndRefund`. Sai o `MessageProcessor.markFailed`.
- Aceite: no `DeadLetterIT`, o saldo volta ao valor inicial e o extrato tem `DEBIT` + `REFUND`.

### Marco 3: republicação periódica
- Migration `V3__add_message_enqueued_at.sql`: coluna `enqueued_at`, backfill com `created_at` e índice parcial para as mensagens não publicadas.
- `MessageRepository.markEnqueued(id, at)` depois de cada publicação bem-sucedida (`MessageService` e `PendingMessagesRecovery`).
- `PendingMessagesRecovery.requeueUnpublished` com `@Scheduled` (`bcb.recovery.unpublished-interval`, padrão 30s): republica as `QUEUED` com `enqueued_at` nulo criadas há mais de `bcb.recovery.unpublished-grace` (padrão 10s).
- `AppConfig` com `@EnableScheduling` e o bean `Clock`, que sai do `SecurityConfig`.
- Aceite: `PendingMessagesRecoveryIT` com publicação falhando no envio e entregue depois que a rotina roda.

## Progresso
- [x] (2026-10-14) Marco 1
- [x] (2026-10-14) Marco 2
- [x] (2026-10-14) Marco 3
- [x] (2026-10-14) Docs (ARCHITECTURE, GLOSSARY, README)

## Descobertas e surpresas
- O `markEnqueued` chamado no `afterCommit` falhava com `InvalidDataAccessApiUsageException: No active transaction for update or delete query`. Nessa fase a transação do envio já terminou, mas continua vinculada à thread, e o `@Transactional` (REQUIRED) do repositório entrava nela. Os testes unitários não pegaram (repositório mockado); o `PendingMessagesRecoveryIT.skipsPublishedMessages` pegou. Correção: `REQUIRES_NEW` no `markEnqueued`.
- Na inicialização, devolver a mensagem para `QUEUED` e zerar `enqueued_at` virou um update só (`resetToUnpublished`). Assim, se a publicação seguinte falhar, a rotina periódica já a encontra, sem precisar de um segundo update no `catch`.
- No teste com docker-compose, a primeira rodada da rotina depois de `docker compose start rabbitmq` ainda falhou (RabbitMQ subindo) e a seguinte, 30s depois, publicou. É o comportamento esperado.

## Registro de decisões
- 2026-10-14: item 2.3 do roadmap (vazão do worker) adiado. A lentidão vem do `Thread.sleep` que simula o envio, não de um gargalo real, então não há o que medir. Subir `concurrency` também quebraria a idempotência atual: dois consumidores podem ler o mesmo id como `QUEUED` ao mesmo tempo. Fazer direito pede um "claim" atômico de `QUEUED` para `PROCESSING` e uma regra para mensagens travadas em `PROCESSING`, o que fica para um plano próprio.
- 2026-10-14: o estorno é decidido por um update condicional (`QUEUED/PROCESSING/SENT → FAILED`). Se a mesma mensagem passar pelo recoverer duas vezes (ex.: DLQ reprocessada), só a primeira estorna.
- 2026-10-14: a coluna `enqueued_at` marca a publicação, em vez de um outbox completo. Assim a rotina periódica só pega mensagens nunca publicadas e não duplica as que já estão na fila esperando a vez. Se o `markEnqueued` falhar depois de publicar, a mensagem é republicada, o que é inofensivo porque o worker ignora mensagem finalizada.
- 2026-10-14: entradas de extrato só para `DEBIT` e `REFUND`. O crédito inicial do cadastro continua em `balance`/`limit` (crédito só no cadastro) e não vira entrada.

## Validação e aceite
- `cd backend && sh ./mvnw verify`
- Com o docker-compose: enviar uma mensagem, `GET /clients/{id}/ledger` com o `DEBIT`, `docker compose stop rabbitmq` → envio fica `QUEUED` → `start rabbitmq` → em até ~30s é entregue sem reiniciar o backend.

## Resultado
Entregue: estorno único no `FAILED` (`failAndRefund` com update condicional), extrato `tb_ledger_entries` com `GET /clients/{id}/ledger` e backfill dos envios antigos, coluna `enqueued_at` e rotina periódica que republica as mensagens nunca publicadas, e `AppConfig` com o `Clock` e o `@EnableScheduling`. `sh ./mvnw verify`: 170 unitários e 49 ITs verdes, 100% das linhas medidas. Com o docker-compose: extrato com o `DEBIT` do backfill e, com o RabbitMQ parado no envio, a mensagem ficou `QUEUED` sem `enqueued_at` e foi entregue cerca de 40s depois de o RabbitMQ voltar, sem reiniciar o backend.
Ficou de fora: o item 2.3 (vazão do worker), ver Registro de decisões. O extrato ainda não aparece no frontend.
