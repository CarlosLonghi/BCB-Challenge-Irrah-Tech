# BCB - Big Chat Brasil

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
| RabbitMQ (painel) | http://localhost:15672 (`guest` / `guest`) |
| PostgreSQL | `localhost:5432` (banco `bcb`, usuário/senha `postgres`) |

Para parar e apagar os dados: `docker compose down -v`.

### Primeiro acesso

Não há cliente cadastrado no início. Abra http://localhost:3000, clique em **Cadastrar cliente**, preencha nome, CPF/CNPJ, plano e saldo/limite: o cadastro já entra no app.

Se preferir a API (endpoint público):

```bash
curl -X POST http://localhost:8080/clients \
  -H "Content-Type: application/json" \
  -d '{"name":"Empresa Exemplo","document":"12345678901","documentType":"CPF","planType":"PRE_PAID","balance":10.00,"limit":0,"active":true}'
```

Depois entre no frontend com o documento `12345678901`.

### Desenvolvimento do frontend (sem Docker)

Com `db`, `rabbitmq` e `backend` rodando (`docker compose up -d --build db rabbitmq backend`):

```bash
cd frontend
npm install
npm run dev     # http://localhost:5173
npm test        # Vitest
npm run lint
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

- **Unitários** (`src/test/java/.../*Test.java`): services, worker, fila, filtro de token, tratamento de erros, controllers e mappers, com caminhos felizes e de erro. Foco na cobrança: débito de saldo (pré-pago) e de limite (pós-pago), recusa sem saldo/limite e nada gravado nem enfileirado quando o envio falha.
- **Integração** (`src/test/java/.../integration/*IT.java`): a aplicação inteira com banco e fila reais, via HTTP (MockMvc). Cobrem o que mock não enxerga:
  - lock pessimista do cliente (uma segunda transação espera a primeira) e envios simultâneos que não gastam o mesmo saldo duas vezes;
  - transação do envio (falha depois do débito desfaz o débito) e o `updateStatus` com `@Transactional` + `@Modifying`;
  - migrations do Flyway, constraints do banco, segurança (401/403), validação (400) e o fluxo completo envio -> RabbitMQ -> worker -> `DELIVERED`;
  - dead-letter queue (`FAILED` depois das tentativas) e recuperação de mensagens pendentes na inicialização.
- **Cobertura:** o JaCoCo exige no mínimo 80% de linhas nos testes unitários (o `verify` falha abaixo disso); hoje está em 100%. O relatório fica em `backend/target/site/jacoco/index.html` depois do `test`/`verify`. Bootstrap, configuração de infraestrutura e DTOs ficam fora da medição.

Para rodar uma classe só: `sh ./mvnw test -Dtest=ClientServiceTest` (unitário) ou `sh ./mvnw verify -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=MessageFlowIT` (integração).

### Frontend

```bash
cd frontend
npm test            # Vitest + Testing Library
```

## Estrutura

```
backend/    API Spring Boot (Java 21, PostgreSQL, RabbitMQ)
frontend/   Interface React
docs/       enunciado e regras do desafio
docker-compose.yaml
```

## Tecnologias

- **Backend:** Java 21, Spring Boot 4.1 (Web, Security, Data JPA, Validation, AMQP, Actuator), PostgreSQL, Flyway, RabbitMQ, MapStruct, Lombok, springdoc-openapi 3 (Swagger)
- **Testes do backend:** JUnit 5, Mockito, AssertJ, Testcontainers (PostgreSQL e RabbitMQ), Awaitility, JaCoCo
- **Frontend:** React 18 + TypeScript + Vite, React Router, CSS Modules, Vitest + Testing Library, ícones Phosphor; servido por nginx no Docker
- **Infra:** Docker Compose

## Funcionalidades

| Funcionalidade | Estado |
|---|---|
| Autenticação por CPF/CNPJ com token Bearer | ✅ |
| Cadastro e consulta de clientes, saldo/limite | ✅ |
| Conversas e histórico de mensagens | ✅ |
| Envio com custo (normal R$ 0,25 / urgente R$ 0,50) e débito de saldo/limite | ✅ |
| Fila com prioridade (urgente antes de normal, FIFO) e ciclo QUEUED -> PROCESSING -> SENT -> DELIVERED | ✅ |
| Fila em RabbitMQ com prioridade, DLQ e reprocessamento | ✅ |
| Frontend: login, cadastro de cliente, conversas, nova conversa, chat, saldo/limite no cabeçalho | ✅ |
| Frontend: layout responsivo | ✅ |
| Frontend: status visuais de mensagem com polling | ✅ |
| Docker Compose do conjunto | ✅ |

## Premissas

- O documento (CPF/CNPJ) identifica o cliente; não há senha, conforme o enunciado.
- O token de sessão é um UUID guardado em memória: reiniciar o backend encerra as sessões.
- O envio é simulado pelo worker (cada mensagem leva cerca de 8 s até `DELIVERED`).
- A atualização de status na tela é feita por polling (a cada 3 s, só enquanto há mensagem pendente); não há WebSocket.
- Para iniciar uma conversa, o contrato exige `recipientId` e `recipientName`. Não há cadastro de destinatários: o backend só armazena o id, e cada envio sem `conversationId` cria uma conversa nova.

## Decisões técnicas e limitações

- **Contrato da API:** documentado no Swagger (`http://localhost:8080/swagger-ui/index.html`). Difere do enunciado em enums em maiúsculas (`PRE_PAID`, `QUEUED`...) e ids numéricos.
- **Envio de mensagem:** débito, mensagem e conversa são gravados numa transação só, com lock pessimista na linha do cliente (`SELECT ... FOR UPDATE`): envios simultâneos do mesmo cliente esperam um pelo outro e não furam o saldo. A mensagem só é publicada na fila depois do commit.
- **Fila:** RabbitMQ com prioridade (`x-max-priority`: urgente 10, normal 1), publicando só o id da mensagem, com um consumidor e `prefetch = 1` (uma mensagem por vez, urgentes passam na frente das normais que estão esperando). Fica atrás da interface `MessageQueue`, então o `MessageService` não conhece AMQP. Começou como `PriorityBlockingQueue` em memória; depois da migração essa versão foi removida, porque o `docker-compose` sempre sobe o RabbitMQ.
- **Falhas no processamento:** 3 tentativas; esgotadas, a mensagem vira `FAILED` e vai para a dead-letter queue `bcb.messages.dlq`. O processamento ignora mensagens já finalizadas, então receber a mesma mensagem duas vezes não a reprocessa.
- **Recuperação:** ao iniciar, o backend devolve para a fila as mensagens que ficaram em `QUEUED`, `PROCESSING` ou `SENT` (backend derrubado no meio, ou publicação que falhou com o RabbitMQ fora do ar).
- **Autenticação e erros:** um filtro lê o `Bearer` token, descobre o cliente e responde `401` antes do controller quando o token falta ou é desconhecido. Erros de negócio viram status HTTP no `ApplicationControllerAdvice`; os que não passam por controller (401 do filtro, rota inexistente) saem pelo `/error` do Spring Boot, no mesmo formato JSON.
- **Frontend sem bibliotecas de dados:** busca com hooks próprios (`useFetch`, `useAction`) em vez de TanStack Query, e máscara de CPF/CNPJ como função pura testada. Menos dependências e código que dá para explicar linha a linha.
- **Status `FAILED` sem reenviar e sem estorno:** o débito acontece no envio e a falha não devolve o valor; um botão de reenvio cobraria de novo.
- **Sem badge de não lidas:** `unreadCount` é sempre `0`, porque o sistema só envia mensagens e não recebe respostas.
- **Visual:** IBM Plex Sans/Mono (números sempre em mono), barra azul-marinho, fundo cinza-papel, um único verde para ações; tokens em `frontend/src/styles/global.css`.
- **Schema:** versionado com Flyway (`backend/src/main/resources/db/migrations`); o Hibernate só valida (`ddl-auto=validate`).
- **Sessões:** o token não expira e não há logout; vale até o backend reiniciar.
- **Testes do backend:** unitários com Mockito para as regras e de integração com Testcontainers para o que depende de Spring, banco e fila reais (lock, transações, queries, segurança, worker). Os de integração rodam no `verify`, então o `test` continua rápido e sem Docker. A meta de cobertura mede só os unitários, para os de integração não esconderem lacunas neles.
- **Não implementado:** tipo de mensagem SMS/WhatsApp, reset mensal do limite pós-pago, histórico de transações financeiras, conversão entre planos, administração de créditos (saldo e limite são definidos só no cadastro).

## Documentação

- [`docs/README.md`](docs/README.md) — visão geral do desafio
- [`docs/fullstack.md`](docs/fullstack.md) — desafio do perfil fullstack
- [`docs/regras-negocio.md`](docs/regras-negocio.md) · [`docs/requisitos-tecnicos.md`](docs/requisitos-tecnicos.md) · [`docs/dicas.md`](docs/dicas.md)
