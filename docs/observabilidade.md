# Observabilidade: métricas, logs e alertas

Guia do padrão de observabilidade deste repositório e de como levá-lo para outro projeto Spring Boot com Docker Compose. Tudo roda no perfil `observability` do compose. Sem esse perfil, nada disto sobe.

```bash
docker compose --profile observability up -d --build
```

| O quê | Onde |
|---|---|
| Grafana | http://localhost:3001 (`admin`/`admin` ou `GRAFANA_ADMIN_*` do `.env`) |
| Prometheus | http://localhost:9090 |
| Loki | só na rede interna; consulte pelo Grafana (Explore → Loki) |

## Arquitetura

```
 serviço Spring Boot ──/actuator/prometheus (porta de gerência)──┐
 RabbitMQ (plugin rabbitmq_prometheus) ──────────────────────────┤
 postgres-exporter ──────────────────────────────────────────────┴──► Prometheus ──┐
                                                                                    ├──► Grafana (dashboards + alertas ──► Discord, opcional)
 stdout de todos os containers do projeto ──► Alloy (socket do Docker) ──► Loki ───┘
```

- **Métricas**: o Prometheus coleta a cada 15s e guarda 15 dias (volume `prometheus_data`).
- **Logs**: o Alloy lê o stdout dos containers do mesmo projeto do compose e manda ao Loki, que guarda 15 dias (volume `loki_data`). A aplicação não sabe que o Loki existe: só escreve JSON no stdout.
- **Traces**: só o `traceId`, nos logs e no header `X-Trace-Id`. Ele passa da requisição HTTP para o worker pelos headers do RabbitMQ. Não há backend de traces (Tempo): o `traceId` serve para juntar os logs.
- **Grafana**: sem volume. Datasources, dashboards e alertas vêm dos arquivos e são recriados a cada subida. O repositório é a fonte da verdade, e o que for mudado pela tela se perde.

## Arquivos

```
observability/
  prometheus.yml                         alvos de coleta (um job por serviço)
  loki.yml                               Loki monolítico, filesystem, retenção 15 dias
  alloy.alloy                            coleta de logs: containers do projeto → labels service/level → Loki
  grafana/
    provisioning/
      datasources/prometheus.yml         uid fixo "prometheus"
      datasources/loki.yml               uid fixo "loki"; traceId vira link para os logs do trace
      dashboards/dashboards.yml          uma pasta do Grafana por subpasta de dashboards/
      alerting/generic.yml               alertas de infraestrutura (pasta Infra)        ← genérico
      alerting/bcb.yml                   alertas do domínio (pasta BCB)                 ← do projeto
    dashboards/
      infra/spring-boot.json             HTTP, JVM, Hikari, rotinas, cliente AMQP, logs ← genérico
      infra/postgres.json                postgres-exporter                              ← genérico
      infra/rabbitmq.json                plugin rabbitmq_prometheus                     ← genérico
      bcb/bcb.json                       mensagens, fila, worker, logs do backend       ← do projeto
    discord/discord.yml                  contact point + política (só com o override)
docker-compose.discord.yaml              override que liga o Discord
```

## Convenções

Os arquivos genéricos funcionam em qualquer projeto que siga estas regras:

1. **`job` do Prometheus = nome do serviço no compose.** Os logs ganham o label `service` com o nome do serviço, então o painel de logs do dashboard Spring Boot usa `{service="$job"}` e mostra os logs do mesmo serviço escolhido no topo.
2. **RabbitMQ em dois jobs:** `<job>` (endpoint agregado) e `<job>-queues` (endpoint `/metrics/detailed`, por fila). O dashboard RabbitMQ monta o segundo nome a partir do primeiro.
3. **Datasources com uid fixo** (`prometheus`, `loki`). Os dashboards e alertas referenciam esses uids, e não nomes.
4. **Toda consulta filtra por `job`.** Aplicação e broker expõem métricas com o mesmo nome (`rabbitmq_connections`).
5. **Labels de log só de baixa cardinalidade:** `service` e `level`. IDs (traceId, id de mensagem) ficam no corpo e são filtrados na consulta (`|= "<id>"` ou `| json`).
6. **Actuator numa porta de gerência separada** (8081), não publicada pelo compose. O Prometheus chega nela pela rede interna.
7. **Métricas de negócio registradas zeradas no startup** (ver `MessageMetrics`): a série existe desde o início, os painéis mostram 0 em vez de "No data", e o `increase()` conta o primeiro evento.
8. **Alertas agrupam `by (job)`:** um alerta por serviço afetado, e o resumo usa `{{ $labels.job }}`.
9. **Dashboards genéricos com a tag `generic`** e os do projeto com a tag `app`. Os menus "Infra" e "Aplicação" no topo de cada dashboard são montados por essas tags.

Cuidado com o `$` nos arquivos de provisioning do Grafana: `${VAR}` vira variável de ambiente. Para um `$` literal, use `$$` (ver `datasources/loki.yml`). `{{ $labels.x }}` nos alertas não é afetado.

## Levar para outro projeto (checklist)

**Na aplicação Spring Boot:**
1. Dependências: `spring-boot-starter-actuator`, `io.micrometer:micrometer-registry-prometheus` (runtime) e, para o `traceId` nos logs, `spring-boot-micrometer-tracing-brave` + `io.micrometer:micrometer-tracing-bridge-brave` (ver `backend/pom.xml`).
2. Em `application.properties`, copie o bloco do Actuator e do tracing (`management.server.port`, `management.endpoints.web.exposure.include=health,prometheus`, histograma HTTP, `management.tracing.sampling.probability`). Com RabbitMQ, copie também os dois `observation-enabled`.
3. No compose, ligue o log JSON com `LOGGING_STRUCTURED_FORMAT_CONSOLE: ecs`. O Alloy lê o nível de `log.level` desse formato.
4. Opcional: o filtro que devolve `X-Trace-Id` (`config/TraceIdHeaderFilter.java`, liberado no CORS em `SecurityConfig`).

**Na infraestrutura:**

5. Copie `observability/` inteira e apague o que é do BCB:
   - `alerting/bcb.yml`;
   - `dashboards/bcb/`;
   - os jobs que não existirem no novo projeto (`rabbitmq`, `postgres`).
6. Copie do `docker-compose.yaml` os serviços `prometheus`, `postgres-exporter` (se usar Postgres), `loki`, `alloy` e `grafana`, além dos volumes `prometheus_data` e `loki_data`. Ajuste:
   - `container_name`;
   - a porta do Grafana no host;
   - `GF_DASHBOARDS_DEFAULT_HOME_DASHBOARD_PATH`;
   - a lista de arquivos de `alerting/` montados.
7. Em `prometheus.yml`, um job por serviço, com o nome do serviço no compose (convenção 1). Com RabbitMQ, crie os dois jobs da convenção 2 e habilite o plugin `rabbitmq_prometheus` (já vem nas imagens `rabbitmq:*-management`).
8. Para o Discord, copie `discord/discord.yml` e `docker-compose.discord.yaml` e troque o uid `bcb-discord`.

**O que é do projeto (fazer de novo):**

9. Um dashboard em `dashboards/<projeto>/` com a tag `app`, o link `{"type":"dashboards","tags":["generic"],"title":"Infra","asDropdown":true}` e as métricas de negócio.
10. Um `alerting/<projeto>.yml` com os alertas do domínio, numa pasta própria.
11. O job `observability` do CI (`.github/workflows/ci.yml`), trocando os uids conferidos e a chamada de fumaça.

## Consultas úteis

| Pergunta | Consulta |
|---|---|
| Tudo o que aconteceu numa requisição (inclusive no worker) | Loki: `{service=~".+"} \|= "<X-Trace-Id>"` (ou clique no traceId de um log) |
| Erros da aplicação | Loki: `{service="backend", level="ERROR"}` |
| Logs de um logger | Loki: `{service="backend"} \| json \| log_logger=~".*MessageProcessor"` |
| Alvos fora | Prometheus: `up == 0` |
| p95 HTTP por rota | Prometheus: `histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket{job="backend"}[5m])))` |

## Alertas

| Regra | Arquivo | Dispara quando |
|---|---|---|
| Alvo fora | `generic.yml` | algum job do Prometheus para de responder por 1 min |
| Postgres fora | `generic.yml` | o exporter responde, mas não conecta no banco |
| Erros 5xx na API | `generic.yml` | mais de 3 respostas 5xx por minuto, por 5 min |
| Pool do banco saturado | `generic.yml` | requisições esperando conexão do Hikari por 2 min |
| Alarme no RabbitMQ | `generic.yml` | alarme de memória ou disco (publicações bloqueadas) |
| Mensagens na DLQ | `bcb.yml` | qualquer mensagem na `bcb.messages.dlq` |
| Entrega URGENT lenta | `bcb.yml` | p95 da entrega URGENT acima de 15s por 5 min |
| Taxa de falha alta | `bcb.yml` | mais de 5% dos envios dos últimos 10 min falharam |

Sem o override do Discord, os alertas aparecem só em **Alerting** e no painel "Alertas disparados" do dashboard BCB.

## O que ficou de fora

- **Traces distribuídos (Tempo):** o `traceId` nos logs já junta requisição e worker. Exportar spans (OTLP → Tempo) permitiria ver a duração de cada etapa e ligar log ↔ trace ↔ métrica (exemplars).
- **Métricas de containers (cAdvisor)** e **do nginx:** CPU e memória por container não são coletadas.
- **Alta disponibilidade:** Prometheus, Loki e Grafana rodam com uma instância cada, com dados em volume local. Serve para desenvolvimento e demonstração, não para produção.
