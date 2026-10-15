# BCB - Big Chat Brasil

[![CI](https://github.com/CarlosLonghi/BCB-Challenge-Irrah-Tech/actions/workflows/ci.yml/badge.svg)](https://github.com/CarlosLonghi/BCB-Challenge-Irrah-Tech/actions/workflows/ci.yml)

Plataforma de chat para comunicação entre empresas e seus clientes, com cobrança por mensagem (pré-pago e pós-pago) e dois níveis de prioridade (normal e urgente). Solução do desafio **Fullstack** da Irrah Tech ([enunciado](https://github.com/irrahgroup/irrah-tech-challenges/blob/main/docs/fullstack.md)).


## Como executar

Requisitos: Docker e Docker Compose.

```bash
git clone <url-do-repositorio>
cd bcb-challenge
docker compose up --build
```

(`docker-compose up --build` também funciona.) A primeira execução demora, porque compila o backend e o frontend.

| Serviço | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui/index.html |
| RabbitMQ (painel) | http://localhost:15672 (`guest` / `guest`, ou os valores do `.env`). Se o login falhar depois de trocar a versão da imagem (`SESSION_EXPIRY is not defined` no console), recarregue sem cache: Ctrl+Shift+R |
| PostgreSQL | `localhost:5432` (banco `bcb`, usuário/senha `postgres`, ou os valores do `.env`) |
| Prometheus (opcional) | http://localhost:9090, com `docker compose --profile observability up -d` |

Para parar e apagar os dados: `docker compose down -v`.

**Volume criado no Postgres 16.** O compose usa o Postgres 18, que monta o volume em `/var/lib/postgresql` e não abre dados da versão 16. Para manter os dados, exporte antes de atualizar (ainda com o 16 rodando) e importe depois:

```bash
docker compose exec db pg_dump -U postgres -Fc bcb > bcb.dump   # ainda no Postgres 16
docker compose down -v                                         # apaga o volume antigo
docker compose up -d --wait db                                 # sobe só o banco, já no 18
docker compose exec -T db pg_restore -U postgres -d bcb --clean --if-exists --no-owner < bcb.dump
docker compose up -d --build --wait
```

Sem dados a preservar, basta `docker compose down -v` antes de subir.

### Configuração (`.env`)

Sem `.env`, o compose usa credenciais e um segredo JWT de desenvolvimento. Para trocar, copie `.env.example` para `.env` na raiz e ajuste os valores: senha do Postgres, usuário e senha do RabbitMQ e `JWT_SECRET`. A senha do Postgres só vale na criação do volume. Com um banco já criado, mudá-la exige `docker compose down -v`, que apaga os dados.

O PostgreSQL, o RabbitMQ e o painel ficam publicados só em `127.0.0.1`. A API (8080) e o frontend (3000) ficam abertos. O Actuator (health e métricas) usa a porta 8081, que não é publicada: dá para consultá-lo com `docker exec bcb-app wget -qO- localhost:8081/actuator/prometheus`.

### Primeiro acesso

Não há cliente cadastrado no início. Abra http://localhost:3000, clique em **Cadastrar cliente**, preencha nome, CPF/CNPJ, senha, plano e saldo/limite: o cadastro já entra no app.

Se preferir a API (endpoint público):

```bash
curl -X POST http://localhost:8080/clients \
  -H "Content-Type: application/json" \
  -d '{"name":"Empresa Exemplo","document":"12345678909","password":"senha-forte-123","documentType":"CPF","planType":"PRE_PAID","balance":10.00,"limit":0,"active":true}'
```

Depois entre no frontend com o documento `12345678909` e a senha `senha-forte-123`.

### Desenvolvimento do frontend (sem Docker)

Com `db`, `rabbitmq` e `backend` rodando (`docker compose up -d --build db rabbitmq backend`):

```bash
cd frontend
npm install
npm run dev     # http://localhost:5173
npm test        # Vitest
npm run lint
npm run api:types  # regenera src/shared/api/schema.d.ts a partir de openapi.json
```

A URL da API vem de `VITE_API_URL` (padrão `http://localhost:8080`; ver `frontend/.env.example`).

### Desenvolvimento do backend (sem Docker)

Com `db` e `rabbitmq` rodando (`docker compose up -d db rabbitmq`):

```bash
cd backend
sh ./mvnw spring-boot:run   # http://localhost:8080
```

## Testes

### Backend

Os testes não usam o `docker compose`: os unitários não precisam de infraestrutura, e os de integração sobem um PostgreSQL e um RabbitMQ descartáveis com Testcontainers (basta o Docker estar ativo).

```bash
cd backend
sh ./mvnw test      # só os unitários (JUnit + Mockito), sem Docker, em poucos segundos
sh ./mvnw verify    # unitários + integração (*IT) + checagem de cobertura; precisa do Docker
```

- **Unitários** (`src/test/java/.../*Test.java`): services, worker, fila, autenticação (senha e JWT), tratamento de erros, controllers e mappers, com caminhos felizes e de erro. Foco na cobrança: débito de saldo (pré-pago) e de limite (pós-pago), recusa sem saldo/limite e nada gravado nem enfileirado quando o envio falha.
- **Integração** (`src/test/java/.../integration/*IT.java`): a aplicação inteira com banco e fila reais, via HTTP (MockMvc). Cobrem o que mock não enxerga:
  - lock pessimista do cliente (uma segunda transação espera a primeira) e envios simultâneos que não gastam o mesmo saldo duas vezes;
  - transação do envio (falha depois do débito desfaz o débito) e o `updateStatus` com `@Transactional` + `@Modifying`;
  - migrations do Flyway, constraints do banco, segurança (401/403), validação (400) e o fluxo completo envio -> RabbitMQ -> worker -> `DELIVERED`;
  - dead-letter queue (`FAILED` e estorno depois das tentativas, sem estornar duas vezes), extrato, recuperação de mensagens pendentes na inicialização e republicação periódica de mensagens que nunca chegaram ao RabbitMQ;
  - stream SSE (`GET /events`): cada etapa chega só ao dono da mensagem, e o `FAILED` só depois do commit do estorno;
  - contrato: `frontend/openapi.json` igual ao `/v3/api-docs` (`OpenApiContractIT`; com `-Dopenapi.write=true` ele regrava o arquivo).
- **Cobertura:** o JaCoCo exige no mínimo 80% de linhas nos testes unitários (o `verify` falha abaixo disso); hoje está em 100%. O relatório fica em `backend/target/site/jacoco/index.html` depois do `test`/`verify`. Bootstrap, configuração de infraestrutura e DTOs ficam fora da medição.

Para rodar uma classe só: `sh ./mvnw test -Dtest=ClientServiceTest` (unitário) ou `sh ./mvnw verify -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=MessageFlowIT` (integração).

### Frontend

```bash
cd frontend
npm test            # Vitest + Testing Library (regras, hooks e as telas de login e chat)
```

E2E com Playwright, contra a aplicação inteira no Docker:

```bash
docker compose up -d --build --wait
cd frontend
npx playwright install chromium   # só na primeira vez
npm run e2e
```

O teste cadastra um cliente, abre uma conversa e espera a mensagem chegar a "Lida" pelo stream de eventos, sem recarregar a página.

### CI e fluxo de contribuição

O GitHub Actions (`.github/workflows/ci.yml`) roda em todo PR para a `main` e em cada push nela, com três jobs:

- **backend:** `mvnw verify` (unitários, integração com Testcontainers e cobertura mínima do JaCoCo); os relatórios ficam como artefato da execução.
- **frontend:** `npm ci`, tipos da API conferidos (`npm run api:types` sem diferença), `lint`, `test` e `build`.
- **e2e:** `docker compose up --build --wait` e o Playwright; em caso de falha, guarda os logs dos containers e o relatório.

A `main` é protegida: não aceita push direto nem force-push. Toda alteração vai numa branch própria e entra por PR, que só pode ser mergeado com os jobs verdes e a branch atualizada com a `main`.

```bash
git checkout -b feat/minha-alteracao
git push -u origin feat/minha-alteracao
gh pr create
```

## Estrutura

```
backend/    API Spring Boot (Java 21, PostgreSQL, RabbitMQ)
frontend/   Interface React
docs/       enunciado e regras do desafio
observability/   configuração do Prometheus (perfil observability do compose)
docker-compose.yaml
.env.example     credenciais e segredo do compose
```

## Tecnologias

- **Backend:** Java 21, Spring Boot 4.1 (Web, Security, Data JPA, Validation, AMQP, Actuator), PostgreSQL, Flyway, RabbitMQ, MapStruct, Lombok, springdoc-openapi 3 (Swagger)
- **Testes do backend:** JUnit 5, Mockito, AssertJ, Testcontainers (PostgreSQL e RabbitMQ), Awaitility, JaCoCo
- **Frontend:** React 19 + TypeScript + Vite, React Router 7, CSS Modules, tipos gerados do OpenAPI (openapi-typescript), Vitest + Testing Library, Playwright, ícones Phosphor; servido por nginx no Docker
- **Infra:** Docker Compose, GitHub Actions (CI), Dependabot
- **Operação:** Micrometer + Prometheus, Micrometer Tracing (Brave) para correlação de logs, logs JSON (ECS), Bucket4j (rate limit)

## Funcionalidades

| Funcionalidade | Estado |
|---|---|
| Autenticação por CPF/CNPJ e senha, com JWT no header Bearer | ✅ |
| Cadastro e consulta de clientes, saldo/limite | ✅ |
| Conversas e histórico de mensagens | ✅ |
| Envio com custo (normal R$ 0,25 / urgente R$ 0,50) e débito de saldo/limite | ✅ |
| Fila com prioridade (urgente antes de normal, FIFO) e ciclo QUEUED -> PROCESSING -> SENT -> DELIVERED -> READ (leitura simulada) | ✅ |
| Fila em RabbitMQ com prioridade, DLQ e reprocessamento | ✅ |
| Frontend: login, cadastro de cliente, conversas, nova conversa, chat, saldo/limite no cabeçalho | ✅ |
| Frontend: layout responsivo | ✅ |
| Frontend: status visuais de mensagem em tempo real (SSE) | ✅ |
| Docker Compose do conjunto | ✅ |

## Premissas

- O login é feito com documento (CPF/CNPJ) e senha. O enunciado não pedia senha: ela entrou depois da entrega. Clientes cadastrados antes disso ficam sem senha e não conseguem entrar.
- A sessão é um JWT (HS256) que vale 8 horas e continua valendo depois de um restart do backend. O segredo vem de `JWT_SECRET`, com pelo menos 32 bytes; o perfil `dev` e o docker-compose têm um valor de desenvolvimento.
- O envio é simulado pelo worker (cada mensagem leva cerca de 8 s até `DELIVERED`).
- A leitura é simulada: cerca de 4 s depois da entrega a mensagem vira `READ`. Isso vale até existir recebimento real de mensagens.
- O status das mensagens chega por SSE (`GET /events`) assim que muda. Se a conexão cair, o frontend reconecta sozinho e, enquanto isso, volta ao polling (a cada 3 s, só da página mais recente e só enquanto alguma mensagem ainda vai mudar).
- As listas são paginadas: o chat mostra as 50 mensagens mais recentes com "Carregar anteriores" (cursor `before`), e a barra lateral mostra 50 conversas com "Carregar mais" (`page`/`size`).
- Para iniciar uma conversa, o contrato exige `recipientId` e `recipientName`. Não há cadastro de destinatários: o backend só armazena o id, e cada envio sem `conversationId` cria uma conversa nova.

## Decisões técnicas e limitações

- **Contrato da API:** documentado no Swagger (`http://localhost:8080/swagger-ui/index.html`). Difere do enunciado em enums em maiúsculas (`PRE_PAID`, `QUEUED`...) e ids numéricos.
- **Envio de mensagem:** débito, mensagem e conversa são gravados numa transação só, com lock pessimista na linha do cliente (`SELECT ... FOR UPDATE`): envios simultâneos do mesmo cliente esperam um pelo outro e não furam o saldo. A mensagem só é publicada na fila depois do commit.
- **Fila:** RabbitMQ com prioridade (`x-max-priority`: urgente 10, normal 1), publicando só o id da mensagem, com um consumidor e `prefetch = 1` (uma mensagem por vez, urgentes passam na frente das normais que estão esperando). Fica atrás da interface `MessageQueue`, então o `MessageService` não conhece AMQP. Começou como `PriorityBlockingQueue` em memória; depois da migração essa versão foi removida, porque o `docker-compose` sempre sobe o RabbitMQ.
- **Falhas no processamento:** 3 tentativas; esgotadas, a mensagem vira `FAILED`, o custo é estornado (ao saldo ou ao limite, uma vez só) e a mensagem vai para a dead-letter queue `bcb.messages.dlq`. O processamento ignora mensagens já finalizadas, então receber a mesma mensagem duas vezes não a reprocessa.
- **Recuperação:** ao iniciar, o backend devolve para a fila as mensagens que ficaram em `QUEUED`, `PROCESSING` ou `SENT` (backend derrubado no meio). A cada 30s, uma rotina republica as mensagens cuja publicação falhou (RabbitMQ fora do ar no envio), identificadas por `enqueued_at` nulo, sem precisar reiniciar o backend.
- **Extrato:** `GET /clients/{id}/ledger` lista os débitos de envio e os estornos (`tb_ledger_entries`), gravados na mesma transação que altera saldo/limite.
- **Autenticação e erros:** a senha é guardada como hash BCrypt. O `POST /auth` emite um JWT com o id do cliente no `sub`, e o resource server do Spring Security valida a assinatura e a expiração em toda rota protegida, respondendo `401` antes do controller. Documento inexistente e senha errada dão a mesma mensagem, para não revelar quais documentos estão cadastrados. Erros de negócio viram status HTTP no `ApplicationControllerAdvice`; os que não passam por controller (401 de token, rota inexistente) saem pelo `/error` do Spring Boot, no mesmo formato JSON.
- **Tempo real:** SSE (`SseEmitter`) em vez de WebSocket, porque a comunicação é só do servidor para o navegador. O stream é lido com `fetch`, e não com `EventSource`, para mandar o token no header `Authorization` em vez de na URL. As conexões ficam em memória, o que basta com uma instância do backend.
- **Imagens Docker:** as duas rodam sem root (backend com o usuário `app`; frontend com `nginx-unprivileged`, na porta 8080 do container) e têm `HEALTHCHECK`. O build do backend baixa as dependências numa camada própria, então mudar só o código não baixa tudo de novo.
- **Observabilidade:**
  - Métricas em `/actuator/prometheus`, na porta de gerência 8081, fora do alcance da rede. Além das do Spring (HTTP, JVM, Hikari, RabbitMQ), há métricas próprias: `bcb_messages_sent_total`, `bcb_messages_delivery_seconds` (da criação até a entrega, por prioridade), `bcb_messages_failed_total`, `bcb_queue_messages` (fila e DLQ) e `bcb_events_connections`.
  - No compose, os logs saem em JSON (ECS) com `traceId`. O id passa pelos headers da mensagem do RabbitMQ, então a requisição que enviou e o worker que entregou aparecem com o mesmo id.
  - Toda resposta traz `X-Trace-Id`.
- **Rate limit:** por IP, com token bucket (Bucket4j): `POST /auth` aceita 10 tentativas por minuto e `POST /clients`, 10 por hora. Acima disso, a resposta é 429 com `Retry-After`. Os buckets ficam em memória, o que basta com uma instância. Atrás de um proxy reverso, é preciso ligar `server.forward-headers-strategy` para o limite valer por cliente.
- **Dependências:** o Dependabot abre PRs semanais (maven, npm, imagens Docker e actions), com minor e patch agrupadas. O major do Node e do `@types/node` fica de fora: sobe à mão, para a próxima LTS.
- **Tipos do frontend gerados do backend:** `frontend/openapi.json` é o contrato versionado. Um teste de integração do backend falha quando ele diverge do `/v3/api-docs`, e o CI falha se `npm run api:types` mudar os tipos gerados. Assim, uma mudança de DTO no backend quebra o build do frontend em vez de passar despercebida.
- **Frontend sem bibliotecas de dados:** busca com hooks próprios (`useFetch`, `useAction`) em vez de TanStack Query, e máscara de CPF/CNPJ como função pura testada. Menos dependências e código que dá para explicar linha a linha.
- **Status `FAILED` sem reenvio:** o valor volta por estorno; um botão de reenvio cobraria de novo.
- **Sem badge de não lidas:** `unreadCount` é sempre `0`, porque o sistema ainda só envia mensagens. Fica no modelo para a troca real de mensagens.
- **Desempenho:** `@ManyToOne` LAZY com open-in-view desligado (sem N+1 nas listas, verificado em teste), índices para as consultas paginadas e `GET /queue/status` numa consulta `GROUP BY` só.
- **Visual:** IBM Plex Sans/Mono (números sempre em mono), barra azul-marinho, fundo cinza-papel, um único verde para ações; tokens em `frontend/src/styles/global.css`.
- **Schema:** versionado com Flyway (`backend/src/main/resources/db/migrations`); o Hibernate só valida (`ddl-auto=validate`).
- **Sessões:** o JWT expira em 8 horas (`bcb.security.jwt.ttl`). Não há refresh token nem revogação no servidor; o logout apaga a sessão do navegador.
- **Configuração:** sem perfil ativo, a aplicação sobe com `application-dev.properties` (SQL no log, DEBUG do Spring Security, segredo JWT de dev). O docker-compose usa o perfil `docker`. As origens do CORS vêm de `CORS_ALLOWED_ORIGINS`.
- **Testes do backend:** unitários com Mockito para as regras e de integração com Testcontainers para o que depende de Spring, banco e fila reais (lock, transações, queries, segurança, worker). Os de integração rodam no `verify`, então o `test` continua rápido e sem Docker. A meta de cobertura mede só os unitários, para os de integração não esconderem lacunas neles.
- **Não implementado:** tipo de mensagem SMS/WhatsApp, reset mensal do limite pós-pago, conversão entre planos, administração de créditos (saldo e limite são definidos só no cadastro).

## Documentação

- [`docs/README.md`](docs/README.md) — visão geral do desafio
- [`docs/fullstack.md`](docs/fullstack.md) — desafio do perfil fullstack
- [`docs/regras-negocio.md`](docs/regras-negocio.md) · [`docs/requisitos-tecnicos.md`](docs/requisitos-tecnicos.md) · [`docs/dicas.md`](docs/dicas.md)
