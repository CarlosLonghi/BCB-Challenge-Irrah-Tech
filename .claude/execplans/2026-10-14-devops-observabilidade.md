# Imagens endurecidas, compose com .env, observabilidade, Dependabot e rate limit

- Status: concluído
- Criado em: 2026-10-14 · Branch: `feat/devops-observabilidade` (a partir de `feat/tempo-real-frontend`)

## Objetivo e contexto
Fase 5 do roadmap (operação e DevOps). Hoje:
- os dois containers rodam como root, o build do backend baixa todas as dependências a cada mudança de código, e só o compose tem healthcheck;
- o Postgres, o RabbitMQ e o painel do RabbitMQ ficam publicados em todas as interfaces, com credenciais fixas no compose;
- os logs são texto sem id de correlação, não há métricas além do `/actuator/health` e nada mede a fila;
- nada atualiza dependências;
- `POST /auth` e `POST /clients` aceitam tentativas sem limite, o que permite força bruta de senha e cadastro em massa.

Resultado esperado:
- imagens rodando sem root, com cache de dependências e `HEALTHCHECK`;
- compose com credenciais vindas de `.env` e portas de infraestrutura só em `127.0.0.1`;
- logs JSON com traceId, que passa da requisição HTTP para o worker pelo RabbitMQ;
- métricas no formato Prometheus, incluindo fila, entrega e SSE;
- Dependabot;
- limite de tentativas por IP nas duas rotas públicas.

## Estado atual
- `backend/Dockerfile`: `mvnw package` copiando `src` junto com o `pom.xml`; imagem final `eclipse-temurin:21-jre-alpine` rodando como root.
- `frontend/Dockerfile`: `nginx:1.27-alpine` na porta 80, como root.
- `docker-compose.yaml`: healthcheck do backend em `:8080/actuator/health`.
- Actuator expõe só `health`, na porta da API.
- `SecurityConfig`: as rotas públicas passam direto, sem nenhum limite.

## Plano de trabalho
### Marco 1: imagens e compose
- Backend:
  - o build baixa as dependências numa camada própria (`dependency:go-offline` depois de copiar só o `pom.xml`);
  - a imagem final roda com o usuário `app`, tem `HEALTHCHECK` e `-XX:MaxRAMPercentage`;
  - `.dockerignore` novo.
- Frontend: `nginxinc/nginx-unprivileged` (porta 8080, sem root), `HEALTHCHECK` e headers de segurança básicos.
- Compose:
  - credenciais em `${VAR:-padrão}`, documentadas num `.env.example` na raiz;
  - Postgres, RabbitMQ e o painel publicados só em `127.0.0.1`;
  - os healthchecks saem do compose e ficam nas imagens.

### Marco 2: observabilidade
- Métricas:
  - Actuator numa porta de gerência separada (`management.server.port`, 8081), que não é publicada pelo compose, com `health` e `prometheus` expostos;
  - `micrometer-registry-prometheus`;
  - métricas próprias: `bcb.messages.sent` (prioridade), `bcb.messages.delivery` (timer da criação até a entrega, por prioridade), `bcb.messages.failed`, `bcb.queue.messages` (mensagens prontas nas filas `bcb.messages` e DLQ) e `bcb.events.connections`.
- Tracing:
  - `micrometer-tracing-bridge-brave`, sem exportador; serve para pôr `traceId`/`spanId` nos logs;
  - observation ligada no `RabbitTemplate` e no listener, para o traceId da requisição seguir até o worker;
  - header `X-Trace-Id` em toda resposta.
- Logs: formato ECS (JSON) no compose, com `LOGGING_STRUCTURED_FORMAT_CONSOLE`. Dev e testes continuam em texto.
- Prometheus no compose, no perfil `observability` (`docker compose --profile observability up`), coletando da porta 8081.

### Marco 3: rate limit
- Bucket4j com os buckets num cache Caffeine (expiram sem uso), por IP:
  - `POST /auth`: 10 por minuto;
  - `POST /clients`: 10 por hora.
- Configurável em `bcb.rate-limit.*` e desligado no perfil `it`.
- Estourou: 429 com `Retry-After` e o JSON de erro padrão.
- O frontend ganha uma mensagem padrão para 429.

### Marco 4: Dependabot
- `.github/dependabot.yml` semanal para maven, npm, docker (as duas imagens e o compose) e github-actions, com as atualizações minor e patch agrupadas.

## Progresso
- [x] (2026-10-14) Marco 1
- [x] (2026-10-14) Marco 2
- [x] (2026-10-14) Marco 3
- [x] (2026-10-14) Marco 4
- [x] (2026-10-14) Docs (ARCHITECTURE, GLOSSARY, README)

## Descobertas e surpresas
- O healthcheck do frontend falhava com `wget http://localhost:8080/`: no container, `localhost` resolve primeiro para `::1`, e o `listen 8080` do nginx só escuta IPv4. Trocado para `127.0.0.1`.
- Com o Actuator na porta 8081, o MockMvc dos ITs recebe 404 em `/actuator/health` (`SecurityIT.publicRoutes` falhou). O perfil `it` usa `management.server.port=8080`, o que põe o Actuator no contexto principal.
- O Spring Boot 4 separou o tracing em módulos: o `spring-boot-micrometer-tracing-brave` não traz a ponte `micrometer-tracing-bridge-brave`, que precisou entrar à parte.
- O caminho feliz do envio e do worker não logava nada, então não havia como ver a correlação. Entraram dois logs INFO: "enfileirada", no `afterCommit`, e "entregue", no worker.
- No formato ECS do Spring Boot, o campo sai como `traceId` (do MDC), não como `trace.id`.

## Registro de decisões
- 2026-10-14: o Actuator vai para uma porta separada, em vez de liberar `/actuator/prometheus` na porta da API. As métricas revelam rotas, volumes e versões, e a porta 8081 fica só na rede interna do compose.
- 2026-10-14: as portas de infraestrutura passam a ser publicadas em `127.0.0.1`, e não removidas. O fluxo de dev (backend ou frontend fora do Docker) continua funcionando, mas ninguém da rede alcança o banco nem o RabbitMQ.
- 2026-10-14: o rate limit usa o IP do `request.getRemoteAddr()`. Atrás de um proxy reverso, é preciso ligar `server.forward-headers-strategy`; sem isso, todos os clientes dividiriam o mesmo bucket.
- 2026-10-14: os buckets ficam em memória (Caffeine), o que basta com uma instância, como no SSE. Com várias, viria o `bucket4j-redis` ou similar.
- 2026-10-14: ficam de fora a publicação de imagem (GHCR) e o deploy do item 5.4, que dependem de onde o projeto vai rodar, e o Grafana. O build das imagens já roda no CI pelo job `e2e`.
- 2026-10-15: o Grafana entrou depois, no mesmo perfil `observability` (:3001, só em `127.0.0.1`), com datasource e dashboard provisionados por arquivo e sem volume.
- 2026-10-16: entram as métricas do broker (plugin `rabbitmq_prometheus`, já ativo na imagem, sem container novo) e do Postgres (`postgres-exporter`), o histograma HTTP no backend e alertas provisionados no Grafana. As consultas filtram por `job`, porque backend e broker têm métricas com o mesmo nome. Os alertas ficam sem contact point: aparecem só na tela até haver um destino (e-mail, Slack).

## Validação e aceite
- `cd backend && sh ./mvnw verify`
- `cd frontend && npm run lint && npm test && npm run build`
- `docker compose up -d --build --wait`:
  - `docker exec bcb-app id` não é root;
  - `curl localhost:8081` falha de fora;
  - `docker exec bcb-app wget -qO- localhost:8081/actuator/prometheus` traz as métricas `bcb_*`;
  - os logs saem em JSON, e o envio e o worker compartilham o `traceId`;
  - a 11ª tentativa de login em 1 minuto recebe 429;
  - `npm run e2e` passa.
- `docker compose --profile observability up -d`: o Prometheus mostra o alvo `backend` como UP.

## Resultado
Entregue:
- Imagens sem root (`app` e `nginx`) com `HEALTHCHECK`; cache de dependências no build do backend; headers de segurança no nginx.
- Compose com `.env` (`.env.example`), infraestrutura publicada só em `127.0.0.1` e Prometheus no perfil `observability`.
- Actuator na porta 8081, com `prometheus`, e as métricas `bcb_*`.
- Tracing Brave com propagação pelo RabbitMQ e `X-Trace-Id`; logs ECS no compose.
- Rate limit com Bucket4j em `POST /auth` e `POST /clients`.
- Mensagem 429 no frontend e Dependabot.

Verificação:
- `sh ./mvnw verify`: 195 unitários e 61 ITs verdes, 100% das linhas medidas.
- Frontend: lint, 70 testes e build.
- Com o docker-compose reconstruído:
  - `id` mostra `app` (uid 100) e `nginx` (uid 101);
  - de fora, a porta 8081 é recusada e `/actuator/health` na 8080 dá 404;
  - o nginx manda os headers de segurança, e a API manda o `X-Trace-Id`;
  - um envio gerou "enfileirada" (requisição) e "entregue" (thread do listener do RabbitMQ) com o mesmo `traceId`, em JSON;
  - o `/actuator/prometheus` interno trouxe `bcb_messages_sent_total`, `bcb_messages_delivery_seconds_*`, `bcb_queue_messages` e `bcb_events_connections`;
  - logins errados: 10 respostas 401 e a 11ª com 429, `Retry-After`, CORS e a mensagem em JSON;
  - o Prometheus marcou o alvo `backend` como UP e respondeu `bcb_messages_sent_total`;
  - `npm run e2e` passou.

Ficaram de fora: a publicação de imagem, o deploy e o Grafana (ver Registro de decisões). O Dependabot só passa a rodar depois do merge na `main`.
