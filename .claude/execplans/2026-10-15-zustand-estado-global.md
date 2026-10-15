# Trocar AuthProvider e AppDataProvider por stores do Zustand

- Status: concluído
- Criado em: 2026-10-15 · Branch: `refactor/zustand-estado-global`

## Objetivo e contexto
Adotar o Zustand junto com os hooks padrão do React, mantendo o frontend sem TanStack Query. Os objetivos são simplicidade, controle sobre os fluxos de dados e um bundle enxuto. O estado compartilhado sai dos Contexts e vai para stores. O estado local das telas continua no `useFetch`/`useAction`.

## Estado atual
- `features/auth/AuthProvider.tsx` + `authContext.ts`: sessão, `expired`, `signIn`/`signOut` e o registro do `setUnauthorizedHandler` num efeito.
- `app/AppDataProvider.tsx` + `appDataContext.ts`: saldo (`useFetch`), conversas (`useConversationPages`) e SSE (`useEventStream`). Fica dentro do `AppLayout`, então o unmount no logout limpa tudo.
- O `value` do `AppDataProvider` era um objeto novo a cada render, e todos os consumidores (`AppHeader`, `ConversationList`, `ChatPage`) re-renderizavam com qualquer mudança.

## Plano de trabalho
### Marco 1: store de auth
- `features/auth/authStore.ts` (sessão, `signIn`, `signOut` com `reset` do appStore, `expire`). O handler de 401 é registrado no próprio módulo.
- `useAuth`/`useSession` leem do store, e os consumidores não mudam.
- Aceite: `npm test`, `npm run lint` e `npm run build` verdes.

### Marco 2: store da tela logada
- `app/appStore.ts`: saldo, conversas paginadas, `connected`, assinantes do SSE (`subscribe`/`emit`) e `reset`. Hooks `useBalance`, `useConversations` e `useEvents`.
- `app/useAppSession.ts`: carga inicial, `connectEvents`, recarga do saldo no `FAILED`/reconexão, e no cleanup fecha o SSE e chama `reset()`.
- Tipos `EventStream`/`StreamListener` movidos para `shared/api/events.ts`. A assinatura do `useMessages` não mudou.
- Aceite: testes, lint e build verdes, mais o `app/appStore.test.ts` novo.

### Marco 3: docs e E2E
- ARCHITECTURE.md (seções "Estado" e "Status em tempo real") e GLOSSARY.md.
- Aceite: `npm run e2e` verde.

## Progresso
- [x] (2026-10-15) Marco 1
- [x] (2026-10-15) Marco 2
- [x] (2026-10-15) Marco 3

## Descobertas e surpresas
- O `afterEach` do arquivo de teste roda antes do `cleanup` registrado no `src/test/setup.ts`. Resetar o `authStore` ali faz a tela ainda montada perder a sessão, e o `useSession` lança erro. Por isso o reset dos stores fica no `beforeEach` de cada arquivo, e não no setup global. Importar os stores no setup também carregaria `features/*/api` antes dos `vi.mock` dos testes.
- O `useConversationPages` não descartava um "Carregar mais" em andamento quando a lista era recarregada (a página da ordem antiga era anexada). O store passou a descartar.

## Registro de decisões
- 2026-10-15: só os Contexts viram stores, e o estado local continua no `useFetch`. Mover mensagens para store traria mais código e mais risco de vazar estado entre conversas, sem ganho claro.
- 2026-10-15: em vez de um contador `generation` único, cada tipo de busca (`balance`, `conversations`, `loadMore`) guarda o id da mais recente. Isso cobre o reset (logout no meio da busca) e também duas buscas seguidas do mesmo tipo, como o `useFetch` fazia.
- 2026-10-15: `useAppSession` como hook chamado no `AppLayout`, no lugar de um Provider. O ciclo de vida (abrir SSE, limpar store) continua preso à montagem do layout.

## Validação e aceite
- `cd frontend && npm run lint && npm test && npm run build`: verde (74 testes).
- `npm run e2e`: verde. Rodou com `E2E_BASE_URL=http://localhost:5173` (Vite dev) contra um backend local, porque a porta 8080 estava ocupada e o `bcb-app` do compose não subiu.

## Resultado
- Zustand 5.0.15. Bundle JS de 300,18 kB (94,62 kB gz) para 300,66 kB (94,86 kB gz), ou seja, +0,24 kB gz.
- Saíram `AuthProvider`, `authContext`, `AppDataProvider`, `appDataContext`, `useEventStream` e `useConversationPages`. Os testes de tela não precisam mais de Provider nem de cast.
- Não foram verificados à mão no navegador: a troca de cliente sem vazamento (o teste "reset descarta..." cobre a parte do store) e a contagem de re-renders no React DevTools.
