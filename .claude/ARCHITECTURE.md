# Arquitetura

Mapa do sistema para quem vai mexer no código. Termos do domínio estão em [GLOSSARY.md](GLOSSARY.md); regras de schema, no [CLAUDE.md](../CLAUDE.md).

## Visão geral

Empresas (clientes) mandam mensagens pagas para destinatários. A API debita o custo, grava a mensagem e publica o id no RabbitMQ; um worker consome a fila por prioridade e atualiza o status no banco, que o frontend acompanha por polling.

```
 navegador
    │
    ▼
 frontend  React + Vite, servido por nginx  (:3000 no Docker, :5173 no dev)
    │  HTTP + Authorization: Bearer <token>
    ▼
 backend   Spring Boot, Java 21              (:8080, Swagger em /swagger-ui/index.html)
    │                         │
    ▼                         ▼
 PostgreSQL (:5432)        RabbitMQ (:5672, painel :15672)
  schema via Flyway          fila bcb.messages (prioridade) ──► worker no próprio backend
                             DLQ  bcb.messages.dlq
```

Tudo sobe com `docker compose up --build` (`docker-compose.yaml`).

## Backend (`backend/src/main/java/com/carloslonghi/bcb`)

| Pacote | O que tem | Onde vai código novo |
|---|---|---|
| `controller` | Controllers REST (`/auth`, `/clients`, `/conversations`, `/messages`, `/queue`) | Endpoint novo: método no controller certo |
| `controller/api/spec` | Interfaces `*Api` com as anotações do Swagger; o controller as implementa | Documentação OpenAPI do endpoint |
| `controller/request`, `controller/response` | DTOs (records) com Bean Validation | Formato de entrada/saída |
| `service` | Regras de negócio; leem o cliente logado do `SecurityContextHolder` | Toda regra nova |
| `repository` | Spring Data JPA | Queries derivadas ou `@Query` |
| `entity`, `entity/enums` | Entidades JPA (`Client`, `Conversation`, `Message`) e enums | Mudou aqui → migration nova (ver CLAUDE.md) |
| `mapper` | MapStruct entidade ↔ DTO | Conversões |
| `infra/queue` | `MessageQueue` (interface), `RabbitMessageQueue`, `PendingMessagesRecovery` | Publicação na fila |
| `infra/worker` | `RabbitMessageListener` (consumidor) e `MessageProcessor` (ciclo de status) | Processamento assíncrono |
| `config` | Segurança, filtro de token, RabbitMQ, Swagger, `ApplicationControllerAdvice` | Infraestrutura e mapeamento de erro |
| `exception` | Exceções de negócio, uma por caso | Erro novo: exceção + handler no advice |
| `utils` | `DocumentValidator` (dígitos de CPF/CNPJ) | Funções puras |

Migrations: `backend/src/main/resources/db/migrations` (hoje só `V1__create_initial_schema.sql`). Config: `backend/src/main/resources/application.properties`.

## Fluxos principais

### Autenticação
1. `POST /auth` com o documento → `AuthService.authenticate` (documento não cadastrado → `DocumentNotRegisteredException` → 401; cliente inativo → 403).
2. `AuthService.createToken` gera um UUID e guarda `token → clientId` num `ConcurrentHashMap` em memória. Sem expiração e sem logout; reiniciar o backend derruba as sessões.
3. `TokenAuthenticationFilter` lê o `Bearer`, põe o `clientId` como principal e responde 401 antes do controller se faltar ou for desconhecido. Rotas livres: `POST /clients`, `/auth`, `/actuator/health`, Swagger e `OPTIONS`.
4. Cada cliente só vê os próprios dados: os services filtram pelo principal (conversa/mensagem de outro cliente vira 404; `/clients/{id}` de outro vira 403).

### Envio de mensagem (`MessageService.sendMessage`, uma transação)
1. `ClientRepository.findWithLockById` → `SELECT ... FOR UPDATE` na linha do cliente.
2. `ConversationService.findOrCreate`: usa `conversationId` ou cria conversa com `recipientId` + `recipientName` (sem eles → `MissingRecipientException`).
3. Custo: `NORMAL` R$ 0,25, `URGENT` R$ 0,50. `ClientService.debit`: pré-pago desconta de `balance`; pós-pago desconta de `limit` (o campo guarda o limite restante). Sem saldo/limite → 402.
4. Grava a `Message` com status `QUEUED` e atualiza o resumo da conversa.
5. `enqueueAfterCommit` publica no `MessageQueue` só depois do commit. Se a publicação falhar, a mensagem fica `QUEUED` no banco e é recuperada no próximo start.

### Processamento
1. `RabbitMessageQueue` publica só o id, com prioridade AMQP 10 (urgente) ou 1 (normal), na fila `bcb.messages` (`x-max-priority` 10).
2. `RabbitMessageListener` consome com `prefetch=1` e um consumidor: uma mensagem por vez, e as urgentes passam na frente das normais que estão esperando.
3. `MessageProcessor.process`: `PROCESSING` → `SENT` → `DELIVERED`, gravando cada etapa com `MessageRepository.updateStatus` (pausa `bcb.worker.step-delay-ms`, padrão 4000; os testes zeram). Mensagem já finalizada é ignorada (idempotente).
4. Exceção → retry do Spring (3 tentativas no total). Esgotadas, o `MessageRecoverer` do `RabbitConfig` marca `FAILED` e rejeita, e a mensagem vai para a `bcb.messages.dlq`. Não há estorno.

### Recuperação na inicialização
`PendingMessagesRecovery` (no `ApplicationReadyEvent`) volta para `QUEUED` e reenfileira o que ficou em `QUEUED`, `PROCESSING` ou `SENT`, por ordem de criação. Falha numa mensagem não impede as outras.

### Erros
`ApplicationControllerAdvice` converte exceções de negócio em `ErrorResponse` (400 validação/documento, 401, 402, 403, 404, 409). O que não passa por controller (401 do filtro, rota inexistente) sai pelo `/error` do Spring Boot no mesmo formato JSON (`spring.web.error.include-message=always`).

### Fila (consulta)
`GET /queue/status` (`QueueStatusService`) conta mensagens por status e prioridade direto no banco, não no RabbitMQ.

## Frontend (`frontend/src`)

| Pasta | O que tem |
|---|---|
| `app/` | `App.tsx` (rotas), `AppDataProvider` (saldo e conversas do cliente logado, compartilhados pela tela), `layout/` (cabeçalho, layout, tela vazia) |
| `features/auth` | Login, cadastro (`registerForm.ts`), `AuthProvider`, `ProtectedRoute` |
| `features/conversations` | Lista, item e nova conversa |
| `features/chat` | `ChatPage`, lista de mensagens, composer, ícone de status, `useMessages` (polling), `useSendMessage`, `messageRules.ts` |
| `features/account` | API de saldo (`GET /clients/{id}/balance`) |
| `shared/api` | `http.ts` (fetch + token + erros), `session.ts`, `types.ts` (tipos do contrato da API) |
| `shared/hooks` | `useFetch` e `useAction`, no lugar de TanStack Query |
| `shared/components`, `shared/utils` | Componentes genéricos; `document.ts` (máscara e validação de CPF/CNPJ), `format.ts` |
| `styles/global.css` | Tokens visuais (cores, fontes IBM Plex) |

Rotas: `/login`, `/cadastro`, e dentro do layout protegido `/`, `/conversas/nova`, `/conversas/:id`. Estilo com CSS Modules (`*.module.css` ao lado do componente). Cada feature tem seu `api.ts`. O polling (`POLLING_INTERVAL_MS = 3000` em `messageRules.ts`) só roda enquanto houver mensagem pendente. URL da API em `VITE_API_URL`.

## Invariantes (não quebrar)

- Débito, mensagem e conversa na mesma transação, com o cliente travado por lock pessimista.
- Publicar na fila só depois do commit.
- O worker relê o estado do banco e ignora mensagem finalizada; receber o mesmo id duas vezes não pode reprocessar.
- Status muda só via `MessageRepository.updateStatus` (não salvar a entidade inteira, que sobrescreveria com uma cópia antiga).
- Schema só muda por migration Flyway nova; Hibernate em `validate`.
- Saldo, limite e plano são definidos só no cadastro; `PUT /clients/{id}` só altera o nome.
- O contrato da API usa enums em maiúsculas e ids numéricos; frontend (`shared/api/types.ts`) e backend precisam andar juntos.

## Testes

| Tipo | Onde | Como rodar |
|---|---|---|
| Unitário backend | `backend/src/test/java/**/*Test.java` (JUnit 5, Mockito, AssertJ) | `cd backend && sh ./mvnw test` (sem Docker) |
| Integração backend | `backend/src/test/java/**/integration/*IT.java`, base `IntegrationTest`, Testcontainers (PostgreSQL + RabbitMQ), `application-it.properties` | `sh ./mvnw verify` (precisa do Docker) |
| Frontend | `*.test.ts(x)` ao lado do código, Vitest + Testing Library | `cd frontend && npm test`, `npm run lint`, `npm run build` |

Helpers: `support/TestData` (fábricas de clientes pré/pós-pago etc.) e `support/AuthenticatedClient` (põe um `clientId` como principal, como o filtro faz). O JaCoCo exige 80% de linhas nos unitários (`verify` falha abaixo disso). Uma classe só: `sh ./mvnw test -Dtest=ClientServiceTest` ou `sh ./mvnw verify -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=MessageFlowIT`.

CI (`.github/workflows/ci.yml`): job backend (`mvnw verify`) e job frontend (`npm ci`, `lint`, `test`, `build`) em todo PR para a `main`, que é protegida (só entra por PR com os dois jobs verdes).
