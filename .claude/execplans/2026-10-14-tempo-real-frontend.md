# Status em tempo real (SSE), dependências, tipos do OpenAPI e testes de tela/E2E

- Status: concluído
- Criado em: 2026-10-14 · Branch: `feat/tempo-real-frontend` (a partir de `feat/banco-desempenho`)

## Objetivo e contexto
Fase 4 do roadmap (experiência e frontend). Hoje:
- o chat consulta o backend a cada 3s enquanto há mensagem pendente;
- o React está na 18, o react-router na 6 e o RabbitMQ na 3;
- `shared/api/types.ts` é escrito à mão, então um campo novo no backend pode passar despercebido no frontend;
- só regras e componentes isolados têm teste, sem nenhum teste da aplicação inteira.

Resultado esperado:
- o status de cada mensagem chega por SSE assim que muda;
- dependências atualizadas;
- tipos do frontend gerados do OpenAPI, com o CI acusando quando ficarem desatualizados;
- testes das telas de login e chat;
- um E2E com Playwright contra o docker-compose.

## Estado atual
- `MessageProcessor` (worker), `ReadSimulator` e `MessageService.failAndRefund` mudam o status com updates de coluna e não avisam ninguém.
- `useMessages` faz polling da página recente enquanto há `QUEUED`/`PROCESSING`/`SENT`/`DELIVERED`. O saldo só é atualizado no envio.
- A autenticação é por `Authorization: Bearer`. O `EventSource` do navegador não manda header.

## Plano de trabalho
### Marco 1: SSE no backend
- Evento `MessageStatusChanged(clientId, conversationId, messageId, status)`, publicado com `ApplicationEventPublisher`:
  - pelo worker (`PROCESSING`, `SENT`, `DELIVERED`);
  - pelo `ReadSimulator` (`READ`), que passa a selecionar as mensagens e marcar por id;
  - pelo `failAndRefund` (`FAILED`).
- `infra/events/MessageEventStream`:
  - Guarda os `SseEmitter` de cada cliente e recebe o evento com `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)`, então o `FAILED` só sai depois do commit do estorno.
  - Manda um heartbeat periódico e remove os emitters que falharem.
- `GET /events` (`text/event-stream`), autenticado como as outras rotas. O `SecurityConfig` libera o dispatch `ASYNC`, que acontece depois de a requisição original já ter sido autorizada.

### Marco 2: SSE no frontend
- `shared/api/events.ts`: lê o stream com `fetch` (para mandar o Bearer), separa os eventos e reconecta com backoff. Um 401 encerra a sessão, como em `request`.
- `AppDataProvider` mantém uma conexão só e expõe `subscribe` e `connected`.
- `useMessages`:
  - aplica o status recebido na mensagem já carregada; se a mensagem não está carregada, recarrega a página recente;
  - a cada reconexão, recarrega a página recente;
  - o polling só roda enquanto o stream estiver desconectado.
- Um `FAILED` recarrega o saldo, já que houve estorno.

### Marco 3: dependências
- React 19 e `@types/react` 19.
- react-router 7: os imports passam de `react-router-dom` para `react-router`.
- RabbitMQ `4-management-alpine` no compose e no Testcontainers.

### Marco 4: tipos gerados do OpenAPI
- `frontend/openapi.json` versionado, sem `servers`.
- O `OpenApiContractIT` compara esse arquivo com o `/v3/api-docs`. Com `-Dopenapi.write=true`, regrava o arquivo.
- Os schemas `*Response` marcam todos os campos como obrigatórios, porque o Jackson sempre os escreve; os campos que podem vir nulos levam `nullable`.
- `npm run api:types` (openapi-typescript) gera `src/shared/api/schema.d.ts`, e `types.ts` passa a ter só os apelidos.
- O CI regenera os tipos e falha se houver diferença.

### Marco 5: testes de tela e E2E
- Vitest: `LoginPage` e `ChatPage`.
- Playwright em `frontend/e2e/`: cadastro, login, nova conversa, envio e status chegando a "Lida" pelo stream, com o saldo descontado.
- Job `e2e` no CI com `docker compose up --build --wait`.

## Progresso
- [x] (2026-10-14) Marco 1
- [x] (2026-10-14) Marco 2
- [x] (2026-10-14) Marco 3
- [x] (2026-10-14) Marco 4
- [x] (2026-10-14) Marco 5
- [x] (2026-10-14) Docs (ARCHITECTURE, GLOSSARY, README)

## Descobertas e surpresas
- Quando o `SseEmitter` vence o timeout, o Spring loga um WARN `AsyncRequestTimeoutException` por conexão, ou seja, um a cada 30min por aba aberta. Visto subindo uma segunda instância com `BCB_EVENTS_TIMEOUT=PT3S`. Correção: `emitter.onTimeout(emitter::complete)`. Na mesma verificação, o dispatch `ASYNC` liberado no `SecurityConfig` fechou a resposta sem `AccessDeniedException`.
- O springdoc 3 gera OpenAPI 3.1, que ignora `@Schema(nullable = true)`. O campo anulável precisa de `types = {"string", "null"}`.
- Sem o `OpenApiCustomizer`, todo campo das respostas sairia opcional (`?`) nos tipos gerados, porque o springdoc só marca como `required` o que tem `@NotNull`/`@NotBlank`. As requisições já vinham certas por causa da Bean Validation.
- O openapi-typescript 7.13 declara `typescript@^5` como peer, e o projeto usa o TS 6. O `overrides` no `package.json` o faz usar o TS 6, e a geração saiu correta.
- O `include` padrão do Vitest pegaria `e2e/*.spec.ts`; ficou restrito a `src/**/*.test.{ts,tsx}`.
- React 19 e react-router 7 entraram sem nenhuma mudança de código além de trocar o import de `react-router-dom` para `react-router`.

## Registro de decisões
- 2026-10-14: o SSE é lido com `fetch`, não com `EventSource`. O `EventSource` não manda header, e pôr o JWT na URL o deixaria em logs de acesso e no histórico.
- 2026-10-14: o polling fica como plano B quando o stream está desconectado (proxy que bufferiza, backend reiniciando), em vez de a tela parar de atualizar.
- 2026-10-14: TanStack Query (item 4.3) fica de fora. Os hooks próprios cobrem o app atual, e com o SSE o estado continua local a cada hook. Trocar a camada de dados inteira é uma mudança à parte, para quando houver mais telas.
- 2026-10-14: os emitters ficam em memória, o que basta com uma instância do backend. Com várias, o evento teria que passar por um broker (ex.: um exchange fanout no RabbitMQ).

## Validação e aceite
- `cd backend && sh ./mvnw verify`
- `cd frontend && npm run lint && npm test && npm run build`
- `docker compose up -d --build --wait && cd frontend && npx playwright test`

## Resultado
Entregue:
- SSE em `GET /events`: `MessageEventStream` com `MessageStatusChanged` publicado pelo worker, pela leitura simulada e pelo estorno (este depois do commit), além de heartbeat e timeout.
- No frontend, o stream é lido com `fetch` e reconecta com backoff. O `useMessages` atualiza o status na hora e só volta ao polling com o stream fora do ar. O saldo é recarregado num `FAILED`.
- Dependências: React 19, react-router 7 e RabbitMQ 4.
- Contrato `frontend/openapi.json` conferido pelo `OpenApiContractIT`, com tipos gerados em `schema.d.ts` e `types.ts` reduzido a apelidos.
- Testes das telas `LoginPage` e `ChatPage` e E2E com Playwright.
- CI com checagem dos tipos gerados e um job `e2e`.

Verificação:
- `sh ./mvnw verify`: 184 unitários e 59 ITs verdes, 100% das linhas medidas.
- Frontend: lint, 70 testes e build.
- Com o docker-compose reconstruído:
  - via curl, chegaram `ready`, `PROCESSING`, `SENT`, `DELIVERED`, `:ping` e `READ`, com o header CORS certo e 401 sem token;
  - o timeout fecha a conexão sem log de erro;
  - `npm run e2e` passou: cadastro, nova conversa e "Lida" sem recarregar, com no máximo 3 buscas do histórico.

Ficou de fora: TanStack Query (ver Registro de decisões). O job `e2e` do CI só roda de verdade no primeiro PR.
