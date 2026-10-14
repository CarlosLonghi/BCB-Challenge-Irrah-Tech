# Glossário

Termos do domínio e onde aparecem no código. O enunciado está em `docs/` (português); o código usa nomes em inglês. Visão do sistema em [ARCHITECTURE.md](ARCHITECTURE.md).

## Domínio

| Termo | No código | Significado |
|---|---|---|
| Cliente | `Client` (`tb_clients`) | Empresa ou pessoa que usa a plataforma e paga pelas mensagens. É quem faz login. Não confundir com o destinatário. |
| Documento | `Client.document`, `ClientDocumentType` (`CPF`, `CNPJ`) | Identifica o cliente e serve de login (não há senha). Só dígitos: 11 (CPF) ou 14 (CNPJ), com dígitos verificadores checados por `DocumentValidator` (backend) e `shared/utils/document.ts` (frontend). Único. |
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
| Status | `MessageStatus` | Ciclo: `QUEUED` → `PROCESSING` → `SENT` → `DELIVERED`; `FAILED` depois de esgotar as tentativas. `READ` existe no enum (e no frontend) mas nada o atribui. `DELIVERED`, `READ` e `FAILED` são finais. |
| Pendente | `QUEUED`, `PROCESSING`, `SENT` | Mensagem que ainda não chegou a um status final; o frontend faz polling enquanto houver uma, e o backend reenfileira essas no start. |
| Não lidas | `Conversation.unreadCount` | Sempre `0`: o sistema só envia, não recebe respostas. |

## Técnico

| Termo | No código | Significado |
|---|---|---|
| Token / sessão | `AuthService` (mapa em memória), `TokenAuthenticationFilter` | UUID devolvido pelo `POST /auth`, mandado como `Authorization: Bearer`. Não expira; some ao reiniciar o backend. |
| Principal | `SecurityContextHolder...getPrincipal()` | O `Long` id do cliente logado; os services filtram tudo por ele. |
| Fila | `MessageQueue`, `RabbitMessageQueue`, fila `bcb.messages` | Fila RabbitMQ com prioridade; carrega só o id da mensagem. |
| DLQ | `bcb.messages.dlq` | Dead-letter queue: onde caem as mensagens que falharam 3 vezes (já marcadas `FAILED`). |
| Worker | `RabbitMessageListener` + `MessageProcessor` | Consumidor que simula o envio e avança o status (cerca de 8 s até `DELIVERED`). |
| Recuperação | `PendingMessagesRecovery` | Reenfileira as mensagens pendentes quando o backend sobe. |
| Status da fila | `GET /queue/status`, `QueueStatusService` | Contagem por status/prioridade feita no banco. |
| Migration | `db/migrations/V<n>__*.sql` | Única forma de mudar o schema (Flyway; Hibernate em `validate`). |
| IT | `*IT.java` | Teste de integração (Testcontainers), roda no `mvnw verify`. |

## Do enunciado, mas não implementado

- Tipo de mensagem SMS/WhatsApp.
- Reset mensal do limite pós-pago.
- Administração de créditos/limites, conversão entre planos e histórico de transações financeiras. Saldo e limite são definidos só no cadastro; `PUT /clients/{id}` altera só o nome.
- Estorno de mensagem `FAILED` e reenvio.
