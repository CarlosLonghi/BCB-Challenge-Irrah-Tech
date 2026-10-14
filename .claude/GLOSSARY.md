# Glossário

Termos do domínio e onde aparecem no código. O enunciado está em `docs/` (português); o código usa nomes em inglês. Visão do sistema em [ARCHITECTURE.md](ARCHITECTURE.md).

## Domínio

| Termo | No código | Significado |
|---|---|---|
| Cliente | `Client` (`tb_clients`) | Empresa ou pessoa que usa a plataforma e paga pelas mensagens. É quem faz login. Não confundir com o destinatário. |
| Documento | `Client.document`, `ClientDocumentType` (`CPF`, `CNPJ`) | Identifica o cliente e, junto com a senha, serve de login. Só dígitos: 11 (CPF) ou 14 (CNPJ), com dígitos verificadores checados por `DocumentValidator` (backend) e `shared/utils/document.ts` (frontend). Único. |
| Ativo / inativo | `Client.active` | Cliente inativo não consegue logar (403). |
| Plano | `ClientPlanType` (`PRE_PAID`, `POST_PAID`) | Forma de cobrança. Definido no cadastro e não muda. |
| Saldo | `Client.balance` | Crédito do pré-pago. Cada envio desconta; sem saldo suficiente → 402. |
| Limite | `Client.limit` (coluna `client_limit`) | Do pós-pago. Guarda o limite **restante**: cada envio desconta dele; não pode ficar negativo (→ 402). |
| Débito | `ClientService.debit` | Desconto do custo no saldo ou no limite, dentro da transação do envio. |
| Destinatário | `recipientId`, `recipientName` | Cliente final que recebe a mensagem. Não tem cadastro: o backend só guarda id e nome na conversa. |
| Conversa | `Conversation` (`tb_conversations`) | Fio de mensagens de um cliente com um destinatário. Guarda o resumo (`lastMessageContent`, `lastMessageTime`). Envio sem `conversationId` cria uma nova. |
| Mensagem | `Message` (`tb_messages`) | Texto enviado pelo cliente (`sender`) numa conversa, com custo, prioridade e status. |
| Prioridade | `MessagePriority` (`NORMAL`, `URGENT`) | Normal custa R$ 0,25; urgente, R$ 0,50 e é processada antes. Na fila vira prioridade AMQP 1 ou 10. |
| Custo | `Message.cost`, `MessageService.NORMAL_COST` / `URGENT_COST` | Valor cobrado no envio, gravado na mensagem. |
| Status | `MessageStatus` | Ciclo: `QUEUED` → `PROCESSING` → `SENT` → `DELIVERED` → `READ`; `FAILED` depois de esgotar as tentativas. `READ` é simulado pelo `ReadSimulator` cerca de 4s depois da entrega (`delivered_at`), até existir recebimento real. Para o worker, `DELIVERED`, `READ` e `FAILED` são finais. |
| Pendente | `QUEUED`, `PROCESSING`, `SENT` | Mensagem que ainda não chegou a um status final; o backend reenfileira essas no start. O frontend faz polling também em `DELIVERED`, esperando o `READ`. |
| Não lidas | `Conversation.unreadCount` | Sempre `0` por enquanto: o sistema só envia. Fica no modelo para quando houver troca real de mensagens. |
| Cursor / página | `before`, `limit`, `page`, `size`; `utils/PageLimit`, `shared/api/pagination.ts` | Paginação das listas: `before` = id exclusivo (mensagens, extrato), `page`/`size` nas conversas. 50 itens por padrão, teto 100. |

## Técnico

| Termo | No código | Significado |
|---|---|---|
| Senha | `Client.passwordHash` (`password_hash`), `PasswordEncoder` (BCrypt) | Definida no cadastro (8 a 72 caracteres); só o hash é gravado. Nula em clientes anteriores à V2, que não conseguem entrar. |
| Token / sessão | `AuthService.createToken`, `JwtConfig`, `oauth2ResourceServer` no `SecurityConfig` | JWT HS256 devolvido pelo `POST /auth` (`sub` = id do cliente), mandado como `Authorization: Bearer`. Expira em 8h (`bcb.security.jwt.ttl`) e continua valendo depois de um restart. |
| Principal | `SecurityContextHolder...getPrincipal()` | O `Long` id do cliente logado; os services filtram tudo por ele. |
| Fila | `MessageQueue`, `RabbitMessageQueue`, fila `bcb.messages` | Fila RabbitMQ com prioridade; carrega só o id da mensagem. |
| DLQ | `bcb.messages.dlq` | Dead-letter queue: onde caem as mensagens que falharam 3 vezes (já marcadas `FAILED` e estornadas). |
| Estorno | `MessageService.failAndRefund`, `ClientService.refund`, `LedgerEntryType.REFUND` | Devolução do custo de uma mensagem que terminou em `FAILED`: ao saldo (pré-pago) ou ao limite (pós-pago). Acontece uma vez só por mensagem. |
| Extrato / lançamento | `LedgerEntry` (`tb_ledger_entries`), `LedgerService`, `GET /clients/{id}/ledger` | Histórico de `DEBIT` (envio) e `REFUND` (estorno), com valor sempre positivo. |
| Publicação | `Message.enqueuedAt` (`enqueued_at`) | Momento em que o id foi publicado no RabbitMQ. Nulo = commit feito, mas a publicação falhou. |
| Worker | `RabbitMessageListener` + `MessageProcessor` | Consumidor que simula o envio e avança o status (cerca de 8 s até `DELIVERED`). |
| Recuperação | `PendingMessagesRecovery` | Reenfileira as mensagens pendentes quando o backend sobe e, a cada 30s, republica as que nunca foram publicadas. |
| Status da fila | `GET /queue/status`, `QueueStatusService` | Contagem por status/prioridade feita no banco. |
| Migration | `db/migrations/V<n>__*.sql` | Única forma de mudar o schema (Flyway; Hibernate em `validate`). |
| IT | `*IT.java` | Teste de integração (Testcontainers), roda no `mvnw verify`. |

## Do enunciado, mas não implementado

- Tipo de mensagem SMS/WhatsApp.
- Reset mensal do limite pós-pago.
- Administração de créditos/limites e conversão entre planos. Saldo e limite são definidos só no cadastro; `PUT /clients/{id}` altera só o nome.
- Reenvio de mensagem `FAILED` (o estorno existe; reenviar cobraria de novo).
