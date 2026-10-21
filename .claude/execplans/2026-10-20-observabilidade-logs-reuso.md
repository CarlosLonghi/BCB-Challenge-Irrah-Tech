# Fechar a observabilidade com logs (Loki) e separar o genérico do domínio

- Status: concluído
- Criado em: 2026-10-20 · Branch: `feat/observabilidade-logs-reuso`

## Objetivo e contexto
Deixar o perfil `observability` completo e reaproveitável em outros projetos:
- logs pesquisáveis no Grafana, com o `traceId` juntando requisição e worker;
- dashboards e alertas genéricos (qualquer Spring Boot, PostgreSQL, RabbitMQ) separados dos do BCB;
- um guia de reuso.

Traces distribuídos (Tempo) ficaram de fora (ver Decisões).

## Estado atual (antes)
- Métricas: Prometheus coletando backend (`backend:8081`), RabbitMQ (jobs `rabbitmq` e `rabbitmq-queues`) e `postgres-exporter`.
- Um dashboard único `observability/grafana/dashboards/bcb.json`, com domínio e infraestrutura juntos e `job="backend"`/`datname="bcb"` fixos.
- Um arquivo de alertas `alerting/bcb.yml` com 8 regras. Não havia alerta de RabbitMQ fora: o painel existia, a regra não.
- Logs ECS com `traceId` no stdout, legíveis só com `docker compose logs`.

## Plano de trabalho
### Marco 1 — Logs
- `observability/loki.yml` (monolítico, filesystem, tsdb v13, retenção 360h) e `observability/alloy.alloy` (containers do projeto pelo socket do Docker → labels `service`/`level` → Loki).
- Serviços `loki` e `alloy` no compose, perfil `observability`. Datasource `provisioning/datasources/loki.yml` com o `traceId` como derived field.
- Aceite: um `POST /messages` devolve `X-Trace-Id`, e `{service=~".+"} |= "<id>"` no Loki traz "enfileirada" e "entregue".

### Marco 2 — Dashboards
- `bcb.json` dividido em `dashboards/infra/{spring-boot,postgres,rabbitmq}.json` (variáveis `job`, `datname`, `queue`) e `dashboards/bcb/bcb.json`. Provider com `foldersFromFilesStructure`.
- Aceite: as 4 dashboards nas pastas Infra e BCB, e todas as consultas com dados (exceto erros HTTP, sem erros no momento).

### Marco 3 — Alertas
- `alerting/generic.yml` (pasta Infra), tudo `by (job)`: alvo fora (`up`), Postgres fora, 5xx, Hikari, alarme do RabbitMQ.
- `alerting/bcb.yml` só com o domínio: DLQ, entrega URGENT lenta, taxa de falha.
- Aceite: `docker compose stop rabbitmq` → "Alvo fora" dispara para `rabbitmq` e `rabbitmq-queues`.

### Marco 4 — CI e docs
- Job `observability`:
  - `loki -verify-config` e `alloy fmt`;
  - os 4 uids de dashboard e o datasource `loki`;
  - fumaça ponta a ponta (envio → traceId no Loki).
- `docs/observabilidade.md` (convenções e checklist), README e ARCHITECTURE.

## Progresso
- [x] (2026-10-20) Marco 1
- [x] (2026-10-20) Marco 2
- [x] (2026-10-20) Marco 3
- [x] (2026-10-20) Marco 4

## Descobertas e surpresas
- A imagem do Loki é distroless (sem `wget`/`curl`), então não dá para ter healthcheck. O Alloy depende só do `service_started` e reenvia o que falhar enquanto o Loki sobe.
- O ECS do Spring Boot aninha o nível: `{"log":{"level":"INFO"}}`. O Alloy extrai com a expressão `log.level`.
- O provisioning do Grafana troca `${...}` por variáveis de ambiente. No derived field do Loki foi preciso usar `$${__value.raw}`. O `{{ $labels.job }}` dos alertas não é afetado (conferido na API).
- O Loki 3 cria sozinho o label `service_name`. Foi desligado (`discover_service_name: []`) para ficar só o `service` do Alloy.
- Com `foldersFromFilesStructure`, o nome da subpasta vira o nome da pasta no Grafana, e uma pasta de alertas com o mesmo nome é reaproveitada. Por isso a subpasta é `infra/`, igual à pasta dos alertas genéricos.

## Registro de decisões
- 2026-10-20 — Loki + Alloy, sem Tempo. Motivo: os logs já trazem o `traceId` propagado até o worker, e buscá-los resolve o caso de uso de debug. Tempo exigiria um exportador OTLP no backend e mais um serviço. Fica como próximo passo no guia.
- 2026-10-20 — Alloy pelo socket do Docker (só leitura), filtrando pelo label `com.docker.compose.project`. Alternativa descartada: o logging driver do Loki, que exige instalar um plugin no Docker de cada máquina.
- 2026-10-20 — Labels só `service` e `level`. O `traceId` fica no corpo, para não explodir a cardinalidade do Loki.
- 2026-10-20 — "Alvo fora" genérico (`min by (job) (up)`) no lugar de "Backend fora". Cobre RabbitMQ (a lacuna) e qualquer job novo sem criar regra.
- 2026-10-20 — Convenção: `job` do Prometheus = serviço do compose = label `service` dos logs. É o que permite o dashboard Spring Boot mostrar métricas e logs do mesmo serviço com uma variável só.

## Validação e aceite
- `docker compose --profile observability config -q`, com e sem o override do Discord.
- `docker compose --profile observability up -d --build --wait` (o frontend não subiu localmente porque a porta 3000 estava ocupada por outro processo; não afeta a observabilidade).
- Consultas dos 4 dashboards conferidas pela API do Grafana com as variáveis preenchidas.
- Envio real com `X-Trace-Id` → 2 linhas no Loki (requisição e worker).
- RabbitMQ parado → "Alvo fora" disparou para `rabbitmq` e `rabbitmq-queues`.

## Resultado
- Entregue:
  - logs no Loki com o link do `traceId`;
  - 3 dashboards genéricos + 1 do domínio;
  - alertas divididos, com o RabbitMQ coberto;
  - CI com fumaça de logs;
  - guia `docs/observabilidade.md`.
- Fora: Tempo, cAdvisor (métricas por container) e métricas do nginx.
