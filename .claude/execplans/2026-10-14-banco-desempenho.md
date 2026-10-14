# Índices, paginação, LAZY, tipos alinhados e leitura simulada

- Status: concluído
- Criado em: 2026-10-14 · Branch: `feat/banco-desempenho` (a partir de `feat/fila-estorno-extrato`)

## Objetivo e contexto
Fase 3 do roadmap (banco e desempenho). As listas devolvem tudo, as FKs não têm índice, os `@ManyToOne` são EAGER (o que gera N+1), o `GET /queue/status` faz 7 `count`, os tipos estão inconsistentes (`LocalDateTime` em `Client`, `recipient_id` INTEGER) e o `READ` nunca é atribuído. Decisão do usuário: manter `READ` e `unreadCount`, porque haverá troca real de mensagens no futuro, e simular a leitura por enquanto. Paginação por cursor com "carregar anteriores" na tela.

## Estado atual
- `ConversationService.findAll`/`findMessages`, `MessageService.findAll` e `LedgerService.findByClient` devolvem listas completas.
- O frontend busca tudo (`useFetch`) e o `useMessages` faz polling enquanto houver `QUEUED`/`PROCESSING`/`SENT`.
- `MessageProcessor` termina em `DELIVERED` via `updateStatus`.
- `spring.jpa.open-in-view` está no padrão (ligado).

## Plano de trabalho
### Marco 1: schema
- `V5__add_indexes.sql`: `tb_conversations(client_id, last_message_time DESC, id DESC)`, `tb_messages(conversation_id, id)`, `tb_messages(sender_id, id)`, `tb_messages(status, priority)`; o índice do extrato passa a ser `(client_id, id DESC)`.
- `V6__align_column_types.sql`: `tb_clients.created_at/updated_at` → `timestamptz` (valores antigos lidos como UTC); `recipient_id` → `BIGINT` nas duas tabelas. No Java: `Instant` e `Long`.
- `V7__add_message_delivered_at.sql`: coluna `delivered_at`, índice parcial `WHERE status = 'DELIVERED'` e backfill das `DELIVERED` antigas com `created_at`.

### Marco 2: LAZY e consultas
- `@ManyToOne(fetch = LAZY)` em `Message.sender`, `Message.conversation` e `Conversation.client`; `spring.jpa.open-in-view=false`. Os mappers só leem ids das associações, o que não inicializa o proxy.
- `QueueStatusService` com uma consulta `GROUP BY status, priority`; `QueueStatusResponse` ganha `read`.

### Marco 3: paginação por cursor
- `limit` (padrão 50, teto 100, valores fora do intervalo são ajustados) e `before=<id>` em `GET /conversations/{id}/messages` (resposta crescente: as `limit` mais recentes antes do cursor), `GET /messages` e `GET /clients/{id}/ledger` (decrescentes). `GET /conversations` usa `page`/`size`, porque a ordem por `lastMessageTime` muda e não serve como cursor estável.
- Frontend: "Carregar anteriores" no chat (mantendo a posição da rolagem), "Carregar mais" nas conversas, e o polling só da página recente.

### Marco 4: leitura simulada
- `MessageProcessor` termina com `markDelivered(id, at)`. `ReadSimulator` (`@Scheduled`, `bcb.simulation.read-interval` 2s) passa para `READ` as `DELIVERED` com `delivered_at` mais antigo que `bcb.simulation.read-after` (4s), num update só.
- Frontend: `DELIVERED` conta como pendente para o polling.

## Progresso
- [x] (2026-10-14) Marco 1
- [x] (2026-10-14) Marco 2
- [x] (2026-10-14) Marco 3
- [x] (2026-10-14) Marco 4
- [x] (2026-10-14) Docs

## Descobertas e surpresas
- A primeira versão do `PaginationIT.noNPlusOne` passava mesmo com EAGER. Todas as mensagens eram da mesma conversa, e o Hibernate carrega a conversa uma vez só por sessão. Refeito com mensagens em 5 conversas: com EAGER falha (7 queries contra 3), com LAZY passa.
- As estatísticas do Hibernate são globais, então o teste também contava os updates de status do worker rodando em paralelo. Ele agora espera todas as mensagens chegarem a `DELIVERED` antes de medir.
- Mudar a assinatura de `ConversationApi.findMessagesByConversationId` com um `replace` de texto pegou primeiro o `findById` (mesmo trecho de `@Parameter`). O compilador acusou porque o controller deixou de implementar o método da interface.

## Registro de decisões
- 2026-10-14: a leitura é simulada por uma rotina agendada, não por mais uma etapa do worker. Uma etapa a mais prenderia o único consumidor por mais 4s por mensagem e se perderia num restart entre `DELIVERED` e `READ`. A rotina faz um update em lote e alcança qualquer `DELIVERED`, inclusive as de antes do restart.
- 2026-10-14: cursor por `id` nas mensagens e no extrato (ids crescem com a criação). As conversas usam `page`/`size` porque são ordenadas por `lastMessageTime`, que muda a cada envio.
- 2026-10-14: `limit` fora de 1..100 é ajustado em vez de responder 400, como fazem APIs públicas comuns (ex.: o `per_page` do GitHub). A resposta continua sendo um array, então o formato não quebra.
- 2026-10-14: `NUMERIC(38,2)` e `VARCHAR(255)` dos enums ficam como estão: mudá-los não traz ganho prático e cada alteração de tipo reescreve a tabela.
- 2026-10-14: `created_at`/`updated_at` antigos de `tb_clients` são convertidos como UTC. O Hibernate gravava o `LocalDateTime` no fuso da JVM, que no Docker é UTC; num banco de dev local o horário dos cadastros antigos pode deslocar algumas horas, o que não afeta regra nenhuma.

## Validação e aceite
- `cd backend && sh ./mvnw verify`
- `cd frontend && npm run lint && npm test && npm run build`
- docker-compose: conversa com mais de 50 mensagens → "Carregar anteriores" traz as antigas sem pular a rolagem; uma mensagem nova chega a "Lida" alguns segundos depois de "Entregue".

## Resultado
Entregue: migrations V5 (índices), V6 (`timestamptz` em `tb_clients`, `recipient_id` BIGINT) e V7 (`delivered_at`); `@ManyToOne` LAZY com open-in-view desligado; `GET /queue/status` com um `GROUP BY` e o campo novo `read`; paginação por cursor (mensagens, extrato) e por página (conversas); "Carregar anteriores" no chat, mantendo a rolagem, e "Carregar mais" nas conversas; leitura simulada pelo `ReadSimulator` e polling também em `DELIVERED`. `sh ./mvnw verify`: 178 unitários e 54 ITs verdes, cobertura do JaCoCo atingida. Frontend: lint, 55 testes e build. Com o docker-compose: V5 a V7 aplicadas, `DELIVERED` antigas viraram `READ`, cursor ok via curl, mensagem nova `PROCESSING → SENT → DELIVERED → READ`.
Não verificado: a tela no navegador (rolagem ao "carregar anteriores"); a lógica de rolagem não tem teste automatizado porque o jsdom não calcula layout.
