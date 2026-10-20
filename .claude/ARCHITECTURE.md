# Arquitetura

Mapa do sistema para quem vai mexer no código. Termos do domínio estão em [GLOSSARY.md](GLOSSARY.md); regras de schema, no [CLAUDE.md](../CLAUDE.md).

## Visão geral

Empresas (clientes) mandam mensagens pagas para destinatários. A API debita o custo, grava a mensagem e publica o id no RabbitMQ; um worker consome a fila por prioridade e atualiza o status no banco. Cada mudança de status chega ao frontend por SSE (`GET /events`).

```
 navegador
    │
    ▼
 frontend  React + Vite, servido por nginx  (:3000 no Docker, :5173 no dev)
    │  HTTP + Authorization: Bearer <token>; status em tempo real por SSE (GET /events)
    ▼
 backend   Spring Boot, Java 21              (:8080, Swagger em /swagger-ui/index.html; Actuator em :8081)
    │                         │
    ▼                         ▼
 PostgreSQL (:5432)        RabbitMQ (:5672, painel :15672)   (publicados só em 127.0.0.1)
  schema via Flyway          fila bcb.messages (prioridade) ──► worker no próprio backend
                             DLQ  bcb.messages.dlq
```

Tudo sobe com `docker compose up --build` (`docker-compose.yaml`; credenciais em `.env`, modelo em `.env.example`). Com `--profile observability`, sobem também um Prometheus (:9090) que coleta do backend em `backend:8081`, do RabbitMQ em `rabbitmq:15692` (plugin `rabbitmq_prometheus`, jobs `rabbitmq` e `rabbitmq-queues`) e do Postgres pelo `postgres-exporter:9187` (`observability/prometheus.yml`), um Loki (só rede interna, 15 dias no volume `loki_data`) que recebe do Alloy os logs de todos os containers do projeto (`observability/alloy.alloy`, socket do Docker, labels `service` e `level`), e um Grafana (:3001) com datasources (uids fixos `prometheus` e `loki`), dashboards e alertas provisionados de `observability/grafana` (os JSONs são a fonte da verdade; a tela não salva mudanças). Dashboards e alertas são separados em genéricos (`dashboards/infra/`: Spring Boot, PostgreSQL e RabbitMQ, parametrizados por `job`; `alerting/generic.yml`, pasta Infra) e do domínio (`dashboards/bcb/bcb.json`, `alerting/bcb.yml`, pasta BCB). Painel ou alerta de negócio novo vai no do domínio; o genérico não cita o BCB. Convenções e checklist de reuso em `docs/observabilidade.md`. Consultas novas no dashboard filtram por `job`: backend e broker expõem nomes iguais (`rabbitmq_connections`, `rabbitmq_channels`). O Prometheus guarda 15 dias no volume `prometheus_data`. O Grafana monta `datasources/`, `dashboards/`, `alerting/generic.yml` e `alerting/bcb.yml` separados, sobre as pastas da imagem, para o `docker-compose.discord.yaml` acrescentar `alerting/discord.yml` (contact point e política, URL em `GRAFANA_DISCORD_WEBHOOK_URL`). O job `observability` do CI valida tudo isso.

## Backend (`backend/src/main/java/com/carloslonghi/bcb`)

| Pacote | O que tem | Onde vai código novo |
|---|---|---|
| `controller` | Controllers REST (`/auth`, `/clients`, `/conversations`, `/messages`, `/queue`) e o stream SSE (`/events`) | Endpoint novo: método no controller certo |
| `controller/api/spec` | Interfaces `*Api` com as anotações do Swagger; o controller as implementa. Cada rota declara as próprias respostas de erro (corpo `ErrorResponse`): com `springdoc.override-with-generic-response=false`, as do `ApplicationControllerAdvice` não entram sozinhas. `@SecurityRequirement` vai em cada método protegido, nunca na interface quando ela tem rota pública | Documentação OpenAPI do endpoint |
| `controller/request`, `controller/response` | DTOs (records) com Bean Validation | Formato de entrada/saída |
| `service` | Regras de negócio; leem o cliente logado do `SecurityContextHolder` | Toda regra nova |
| `repository` | Spring Data JPA | Queries derivadas ou `@Query` |
| `entity`, `entity/enums` | Entidades JPA (`Client`, `Conversation`, `Message`) e enums | Mudou aqui → migration nova (ver CLAUDE.md) |
| `mapper` | MapStruct entidade ↔ DTO | Conversões |
| `infra/queue` | `MessageQueue` (interface), `RabbitMessageQueue`, `PendingMessagesRecovery` (inicialização + rotina periódica) | Publicação na fila |
| `infra/worker` | `RabbitMessageListener` (consumidor), `MessageProcessor` (ciclo de status) e `ReadSimulator` (leitura simulada) | Processamento assíncrono |
| `infra/events` | `MessageStatusChanged` (evento de domínio) e `MessageEventStream` (conexões SSE por cliente) | Aviso em tempo real ao frontend |
| `infra/metrics` | `MessageMetrics` (contadores e timer do ciclo da mensagem) | Métrica de negócio nova |
| `config` | Segurança (`SecurityConfig`, `JwtConfig`, `JwtProperties`, `RateLimitFilter`/`RateLimitProperties`), `TraceIdHeaderFilter`, `AppConfig` (`Clock`, `@EnableScheduling`), RabbitMQ, Swagger, `ApplicationControllerAdvice` | Infraestrutura e mapeamento de erro |
| `exception` | Exceções de negócio, uma por caso | Erro novo: exceção + handler no advice |
| `utils` | `DocumentValidator` (dígitos de CPF/CNPJ), `PageLimit` | Funções puras |

Migrations: `backend/src/main/resources/db/migrations` (`V1__create_initial_schema.sql`, `V2__add_client_password.sql`, `V3__add_message_enqueued_at.sql`, `V4__create_ledger_entries.sql`, `V5__add_indexes.sql`, `V6__align_column_types.sql`, `V7__add_message_delivered_at.sql`). Config: `backend/src/main/resources/application.properties`, mais `application-dev.properties`, o perfil padrão quando nenhum está ativo (SQL no log, DEBUG do Security, segredo JWT de dev).

## Fluxos principais

### Autenticação
1. `POST /clients` grava a senha como hash BCrypt (`ClientService.create`, `PasswordEncoder` definido no `SecurityConfig`).
2. `POST /auth` com documento e senha → `AuthService.authenticate`. Documento inexistente, senha errada ou cliente sem senha → `InvalidCredentialsException` → 401, sempre com a mesma mensagem. Cliente inativo → 403.
3. `AuthService.createToken` emite um JWT HS256 (`sub` = clientId, `exp` = agora + `bcb.security.jwt.ttl`), assinado com `bcb.security.jwt.secret` (`JWT_SECRET`, mín. 32 bytes, checado em `JwtProperties`). Os beans de encoder e decoder ficam em `JwtConfig`. Não há estado no servidor: o token continua valendo depois de um restart, e não há revogação.
4. O `oauth2ResourceServer` do `SecurityConfig` valida o `Bearer`, põe o `clientId` (`Long`) como principal e responde 401 antes do controller se o token faltar, for inválido ou estiver vencido. As rotas livres ficam só no `SecurityConfig`: `POST /clients`, `POST /auth`, `/actuator/health` e `/actuator/prometheus` (que só existem na porta de gerência 8081), Swagger e `OPTIONS`.
   - Rate limit: o `RateLimitFilter` entra na cadeia logo depois do CORS, para o 429 sair com `Access-Control-Allow-Origin`. É um token bucket por IP (Bucket4j, buckets num cache Caffeine): `POST /auth` 10 por minuto, `POST /clients` 10 por hora (`bcb.rate-limit.*`). Estourou: 429 com `Retry-After`, pelo `/error`. Fica desligado no perfil `it`. Não é um bean, para o Boot não o registrar duas vezes como filtro.
5. Cada cliente só vê os próprios dados: os services filtram pelo principal (conversa/mensagem de outro cliente vira 404; `/clients/{id}` de outro vira 403).

### Envio de mensagem (`MessageService.sendMessage`, uma transação)
1. `ClientRepository.findWithLockById` → `SELECT ... FOR UPDATE` na linha do cliente.
2. `ConversationService.findOrCreate`: usa `conversationId` ou cria conversa com `recipientId` + `recipientName` (sem eles → `MissingRecipientException`).
3. Custo: `NORMAL` R$ 0,25, `URGENT` R$ 0,50. `ClientService.debit`: pré-pago desconta de `balance`; pós-pago desconta de `limit` (o campo guarda o limite restante). Sem saldo/limite → 402.
4. Grava a `Message` com status `QUEUED`, registra o `DEBIT` no extrato (`LedgerService.record`) e atualiza o resumo da conversa.
5. `enqueueAfterCommit` publica no `MessageQueue` só depois do commit e grava `enqueued_at` (`MessageRepository.markEnqueued`, com `REQUIRES_NEW`, porque no `afterCommit` a transação original já terminou). Se a publicação falhar, a mensagem fica `QUEUED` com `enqueued_at` nulo, e a rotina periódica da recuperação a publica de novo.

### Processamento
1. `RabbitMessageQueue` publica só o id, com prioridade AMQP 10 (urgente) ou 1 (normal), na fila `bcb.messages` (`x-max-priority` 10).
2. `RabbitMessageListener` consome com `prefetch=1` e um consumidor: uma mensagem por vez, e as urgentes passam na frente das normais que estão esperando.
3. `MessageProcessor.process`: `PROCESSING` → `SENT` → `DELIVERED`, gravando cada etapa com `MessageRepository.updateStatus` e a entrega com `markDelivered`, que grava também `delivered_at` (pausa `bcb.worker.step-delay-ms`, padrão 4000; os testes zeram). Mensagem já finalizada é ignorada (idempotente).
4. Leitura simulada: o `ReadSimulator` (`@Scheduled`, `bcb.simulation.read-interval` 2s) seleciona as `DELIVERED` com `delivered_at` anterior a `bcb.simulation.read-after` (4s), até 500 por rodada (`findDeliveredBefore`), e as passa para `READ` num update só (`markRead`). Ele fica fora do worker para não prender o consumidor e para alcançar também as mensagens entregues antes de um restart. Sai quando houver recebimento real de mensagens (`READ` e `unreadCount` ficam no modelo por isso).
5. Exceção → retry do Spring (3 tentativas no total). Esgotadas, o `MessageRecoverer` do `RabbitConfig` chama `MessageService.failAndRefund` e rejeita, e a mensagem vai para a `bcb.messages.dlq`. O `failAndRefund` (uma transação) trava o cliente, passa a mensagem de `QUEUED`/`PROCESSING`/`SENT` para `FAILED` com um update condicional (`updateStatusIfIn`) e, só se essa transição aconteceu, devolve o `cost` (`ClientService.refund`) e registra o `REFUND`. Uma segunda passagem pela mesma mensagem não estorna de novo.
6. Cada etapa (`PROCESSING`, `SENT`, `DELIVERED`, `READ`, `FAILED`) publica um `MessageStatusChanged` (ver Tempo real).

### Tempo real (SSE)
- `GET /events` (`EventController`) devolve um `SseEmitter` registrado no `MessageEventStream`, por cliente logado (uma conexão por aba). O primeiro evento é `ready`; os seguintes, `message-status`, com `{messageId, conversationId, status}` (`MessageStatusEventResponse`).
- Quem muda o status publica `MessageStatusChanged` com o `ApplicationEventPublisher`: `MessageProcessor`, `ReadSimulator` e `MessageService.failAndRefund`. O `MessageEventStream` recebe com `@TransactionalEventListener(fallbackExecution = true)`. Dentro de transação (o estorno), o evento sai só depois do commit; fora dela (o worker), sai na hora.
- A conexão fecha depois de `bcb.events.timeout` (30min), e o frontend reconecta; um token vencido recebe 401 nessa hora. A cada `bcb.events.heartbeat` (20s) vai um comentário `:ping`, que mantém a conexão viva em proxies e tira da lista as que caíram.
- O `SecurityConfig` libera o dispatch `ASYNC` (`dispatcherTypeMatchers`), que acontece no fim da resposta assíncrona sem o token. A requisição original já passou pela autenticação.
- As conexões ficam em memória: com mais de uma instância do backend, o evento teria que passar por um broker.

### Extrato
`tb_ledger_entries` (`LedgerEntry`, `LedgerService`) tem um `DEBIT` por envio e um `REFUND` por mensagem que falhou, sempre gravados na mesma transação que altera saldo/limite. `GET /clients/{id}/ledger` lista do mais recente para o mais antigo; outro cliente → 403. O crédito inicial do cadastro não vira lançamento.

### Recuperação
`PendingMessagesRecovery` tem duas rotinas. Falha numa mensagem não impede as outras.
- **Na inicialização** (`ApplicationReadyEvent`): o que ficou em `QUEUED`, `PROCESSING` ou `SENT` volta para `QUEUED` sem publicação (`resetToUnpublished`) e é reenfileirado por ordem de criação.
- **Periódica** (`@Scheduled`, `bcb.recovery.unpublished-interval`, padrão 30s; `@EnableScheduling` no `AppConfig`): republica as `QUEUED` com `enqueued_at` nulo criadas há mais de `bcb.recovery.unpublished-grace` (10s). Ou seja, só as que nunca chegaram ao RabbitMQ, e não as que estão na fila esperando a vez.

### Erros
`ApplicationControllerAdvice` converte exceções de negócio em `ErrorResponse` (400 validação/documento, 401, 402, 403, 404, 409). O que não passa por controller (401 de token, rota inexistente) sai pelo `/error` do Spring Boot no mesmo formato JSON (`spring.web.error.include-message=always`).

### Observabilidade
- Actuator em `management.server.port` (8081), expondo `health` e `prometheus`. No perfil `it` ele fica na porta da aplicação, porque o MockMvc não alcança a porta separada.
- Métricas próprias:
  - `MessageMetrics`: `bcb.messages.sent`, contado no `afterCommit` do envio; `bcb.messages.delivery`, timer da criação até `DELIVERED`, registrado no `MessageProcessor`; `bcb.messages.failed`, registrado no `failAndRefund`. Todas por prioridade.
  - `QueueMetrics` (`MeterBinder`): `bcb.queue.messages`, lido do RabbitMQ a cada coleta.
  - O `MessageEventStream` também é `MeterBinder`: `bcb.events.connections`.
- Tracing (Micrometer Tracing + Brave, sem exportador): serve para correlacionar os logs.
  - `spring.rabbitmq.template/listener.simple.observation-enabled` levam o traceId nos headers da mensagem, então o log "enfileirada" (requisição) e o "entregue" (worker) saem com o mesmo `traceId`.
  - O `TraceIdHeaderFilter` (ordem `HIGHEST_PRECEDENCE + 2`, antes do Security) devolve o id em `X-Trace-Id`.
- Logs: texto em dev e nos testes; JSON ECS no compose (`LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`). No perfil `observability`, o Alloy os leva ao Loki; o nível vem de `log.level` do ECS e vira o label `level`. O `traceId` fica no corpo (não é label) e o datasource do Loki o transforma em link para `|= "<traceId>"`.

### Fila (consulta)
`GET /queue/status` (`QueueStatusService`) conta mensagens por status e prioridade direto no banco (um `GROUP BY status, priority` em `MessageRepository.countByStatusAndPriority`), não no RabbitMQ.

### Paginação
- As listas devolvem arrays de no máximo `limit` itens (padrão 50, ajustado para 1..100 por `utils/PageLimit`, sem 400).
- Por cursor `before=<id>` (exclusivo): `GET /conversations/{id}/messages` devolve as mais recentes antes do cursor em ordem **crescente**; `GET /messages` e `GET /clients/{id}/ledger` em ordem decrescente.
- `GET /conversations` usa `page`/`size`, porque a ordem por `lastMessageTime` muda a cada envio e não serve de cursor.
- Índices dessas consultas na `V5__add_indexes.sql`.

### JPA
`@ManyToOne` são `LAZY` e `spring.jpa.open-in-view=false`: associação lida fora de transação falha em vez de virar query escondida. Os mappers só leem o id das associações (`conversation.id`, `sender.id`, `client.id`, `message.id`), o que não inicializa o proxy. O `PaginationIT.noNPlusOne` garante que listar mensagens de várias conversas não faz uma query por conversa.

## Frontend (`frontend/src`)

| Pasta | O que tem |
|---|---|
| `app/` | `App.tsx` (rotas), `appStore.ts` (store Zustand com saldo, conversas paginadas e a conexão SSE do cliente logado, mais os hooks `useBalance`, `useConversations` e `useEvents`), `useAppSession.ts` (carga inicial, SSE e limpeza do store enquanto o layout está montado), `layout/` (cabeçalho, layout, tela vazia) |
| `features/auth` | Login, cadastro (`registerForm.ts`), `authStore.ts` (store Zustand da sessão; registra o handler de 401), `useAuth`/`useSession`, `ProtectedRoute` |
| `features/conversations` | Lista, item e nova conversa |
| `features/chat` | `ChatPage`, lista de mensagens ("Carregar anteriores", rolagem preservada), composer, ícone de status, `useMessages` (página recente atualizada pelo SSE + páginas antigas), `useSendMessage`, `messageRules.ts` |
| `features/account` | API de saldo (`GET /clients/{id}/balance`) |
| `shared/api` | `http.ts` (fetch + token + erros), `events.ts` (leitura do SSE com fetch e reconexão), `session.ts`, `schema.d.ts` (gerado, não editar) e `types.ts` (apelidos dos schemas) |
| `shared/hooks` | `useFetch` e `useAction`, no lugar de TanStack Query, para o estado local de cada tela |
| `shared/components`, `shared/utils` | Componentes genéricos; `document.ts` (máscara e validação de CPF/CNPJ), `format.ts` |
| `styles/global.css` | Tokens visuais (cores, fontes IBM Plex) |

Estado:
- Compartilhado entre partes da tela (sessão, saldo, conversas, conexão SSE): stores do Zustand (`authStore`, `appStore`). Os componentes leem por seletor (com `useShallow` quando o seletor monta objeto), então só re-renderizam quando o pedaço que usam muda.
- Local de uma tela (mensagens, conversa aberta, envio, formulários): `useFetch`/`useAction`. Não mova para o store sem motivo.
- O `appStore` é global e sobrevive ao unmount: o `reset()` roda no cleanup do `useAppSession` (ao sair do layout) e no `signOut`, para o próximo cliente não ver dados do anterior. Cada busca do store tem um id, e só a mais recente de cada tipo grava; o `reset` invalida as que estão no ar (o equivalente à flag `ignore` do `useFetch`).
- Nos testes, os stores são globais: resete com `useXStore.setState(useXStore.getInitialState(), true)` no `beforeEach` (no `afterEach` do arquivo a tela ainda está montada e quebraria).

Rotas: `/login`, `/cadastro`, e dentro do layout protegido `/`, `/conversas/nova`, `/conversas/:id`. Estilo com CSS Modules (`*.module.css` ao lado do componente). Cada feature tem seu `api.ts`.

Status em tempo real:
- O `useAppSession` abre uma conexão SSE (`shared/api/events.ts`) com `fetch`, e não com `EventSource`, para mandar o `Authorization`. Se a conexão cair, reconecta com espera de 1s a 30s, e um 401 encerra a sessão.
- O `useMessages` assina os eventos (`useEvents()` do `appStore`). O de uma mensagem já carregada troca o status na hora; o de uma mensagem mais nova que a página recarrega a página recente. A cada reconexão (`ready` com `reconnected`), recarrega também.
- O `useAppSession` recarrega o saldo num `FAILED` (houve estorno) e a cada reconexão.
- Só com o stream fora do ar volta o polling (`POLLING_INTERVAL_MS = 3000` em `messageRules.ts`): rebusca a página recente enquanto houver mensagem que ainda vai mudar (`QUEUED`, `PROCESSING`, `SENT` ou `DELIVERED`).

Tipos da API: `frontend/openapi.json` é o contrato versionado (o `OpenApiContractIT` do backend falha se ele diferir do `/v3/api-docs`). `npm run api:types` gera `schema.d.ts` a partir dele com o openapi-typescript; o `overrides` do `package.json` o faz usar o TypeScript 6 do projeto. O `OpenApiCustomizer` do `SwaggerConfig` marca todos os campos dos `*Response` como obrigatórios; os que podem vir nulos declaram `types = {"string", "null"}`. As conversas da barra lateral são paginadas no `app/appStore.ts` ("Carregar mais"); `shared/api/pagination.ts` tem o `PAGE_SIZE` e o `mergeById`. URL da API em `VITE_API_URL`.

## Invariantes (não quebrar)

- Débito, mensagem e conversa na mesma transação, com o cliente travado por lock pessimista.
- Publicar na fila só depois do commit.
- O worker relê o estado do banco e ignora mensagem finalizada; receber o mesmo id duas vezes não pode reprocessar.
- Status muda só por updates de coluna do `MessageRepository` (`updateStatus`, `updateStatusIfIn`, `resetToUnpublished`, `markDelivered`, `markRead`), nunca salvando a entidade inteira, que sobrescreveria com uma cópia antiga.
- Toda mudança de status visível ao cliente publica `MessageStatusChanged`; sem isso, a tela só atualiza num polling de reserva ou numa reconexão.
- Saldo/limite e extrato andam juntos: todo `debit`/`refund` grava o lançamento na mesma transação. Estorno só depois da transição condicional para `FAILED`.
- Schema só muda por migration Flyway nova; Hibernate em `validate`.
- `@ManyToOne` sempre `LAZY`, com open-in-view desligado; lista nova sempre paginada.
- Saldo, limite e plano são definidos só no cadastro; `PUT /clients/{id}` só altera o nome.
- O contrato da API usa enums em maiúsculas e ids numéricos. Mudou um DTO ou endpoint: regrave `frontend/openapi.json` (`sh ./mvnw verify -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=OpenApiContractIT -Dopenapi.write=true`) e rode `npm run api:types` no mesmo PR. O CI confere os dois.

## Testes

| Tipo | Onde | Como rodar |
|---|---|---|
| Unitário backend | `backend/src/test/java/**/*Test.java` (JUnit 5, Mockito, AssertJ) | `cd backend && sh ./mvnw test` (sem Docker) |
| Integração backend | `backend/src/test/java/**/integration/*IT.java`, base `IntegrationTest`, Testcontainers (PostgreSQL + RabbitMQ), `application-it.properties` | `sh ./mvnw verify` (precisa do Docker) |
| Frontend | `src/**/*.test.ts(x)` ao lado do código, Vitest + Testing Library (inclui as telas `LoginPage` e `ChatPage`) | `cd frontend && npm test`, `npm run lint`, `npm run build` |
| E2E | `frontend/e2e/*.spec.ts`, Playwright (Chromium) contra o docker-compose | `docker compose up -d --build --wait`, depois `cd frontend && npm run e2e` (na primeira vez, `npx playwright install chromium`) |

Helpers: `support/TestData` (fábricas de clientes pré/pós-pago etc.) e `support/AuthenticatedClient` (põe um `clientId` como principal, como o resource server faz). Nos ITs, `IntegrationTest.login` usa a senha `PASSWORD` dos clientes criados por `createClient`. O JaCoCo exige 80% de linhas nos unitários (`verify` falha abaixo disso). Uma classe só: `sh ./mvnw test -Dtest=ClientServiceTest` ou `sh ./mvnw verify -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=MessageFlowIT`.

Dependabot (`.github/dependabot.yml`): PRs semanais de maven, npm, imagens Docker (Dockerfiles e compose) e actions, com minor e patch agrupadas. Ignora o major da imagem `node` e do `@types/node`, que sobem juntos e à mão, só para versões LTS.

CI (`.github/workflows/ci.yml`), em todo PR para a `main`, que é protegida (só entra por PR com os jobs verdes):
- backend: `mvnw verify`;
- frontend: `npm ci`, os tipos gerados conferidos com `git diff`, `lint`, `test` e `build`;
- e2e: `docker compose up --build --wait` e Playwright.
