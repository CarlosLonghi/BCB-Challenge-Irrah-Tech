# Adicionar senha ao login e trocar o token em memória por JWT

- Status: concluído
- Criado em: 2026-10-14 · Branch: `feat/auth-senha-jwt`

## Objetivo e contexto
Hoje basta saber o CPF/CNPJ de um cliente para entrar na conta dele, e os tokens são UUIDs num `ConcurrentHashMap`: não expiram, e um restart do backend derruba todas as sessões. O objetivo é ter login com documento + senha (BCrypt) e um JWT assinado com expiração, além de separar a configuração de dev (SQL no log, DEBUG do Security) da configuração padrão.

## Estado atual
- `service/AuthService.java`: `authenticate(document)` busca pelo documento e recusa cliente inativo; `createToken`/`getClientIdFromToken` usam o mapa em memória.
- `config/TokenAuthenticationFilter.java`: lê o `Bearer`, põe o `clientId` (Long) como principal e responde 401 com mensagem própria. Tem uma lista própria de rotas públicas (`shouldNotFilter`), separada da que está em `SecurityConfig`.
- Os services leem o principal como `(Long) ...getPrincipal()`. Esse contrato precisa continuar valendo.
- `DocumentNotRegisteredException` → 401 "Documento não cadastrado", o que permite descobrir quais documentos estão cadastrados.
- `application.properties` tem `show-sql=true` e `logging.level.org.springframework.security=DEBUG`; o CORS fica fixo em `SecurityConfig`.

## Plano de trabalho
### Marco 1: senha no cadastro e no login (backend)
- Migration `V2__add_client_password.sql`: coluna `password_hash VARCHAR(100)` que aceita nulo. Clientes antigos ficam sem senha e não conseguem entrar.
- `Client.passwordHash`; `ClientRequest.password` e `AuthRequest.password` (8 a 72 caracteres, o limite do BCrypt); `PasswordEncoder` BCrypt; `ClientService.create` grava o hash.
- `AuthService.authenticate(document, password)`: documento inexistente, senha errada ou cliente sem senha → `InvalidCredentialsException` (401, "Documento ou senha inválidos."). A exceção substitui a `DocumentNotRegisteredException`.
- Aceite: `sh ./mvnw test` verde.

### Marco 2: JWT
- Dependência `spring-boot-starter-security-oauth2-resource-server`. `JwtConfig` com `NimbusJwtEncoder`/`NimbusJwtDecoder` HS256 a partir de `bcb.security.jwt.secret` (mín. 32 bytes) e `bcb.security.jwt.ttl` (padrão 8h).
- `AuthService.createToken` emite o JWT (`sub` = clientId, `exp`). `SecurityConfig` usa `oauth2ResourceServer(jwt)` com um conversor que põe o `Long` como principal e um entry point que responde 401 com as mesmas mensagens de antes (`/error` em JSON).
- Remover `TokenAuthenticationFilter`: as rotas públicas passam a existir num lugar só.
- Aceite: `sh ./mvnw verify` verde (o `SecurityIT` cobre o token expirado e o adulterado).

### Marco 3: configuração por ambiente
- `application-dev.properties` (perfil padrão via `spring.profiles.default=dev`) com `show-sql`, DEBUG e o segredo JWT de dev. A base exige `JWT_SECRET`.
- CORS em `bcb.cors.allowed-origins`. No docker-compose: `SPRING_PROFILES_ACTIVE=docker`, `JWT_SECRET` e `CORS`.

### Marco 4: frontend e docs
- Campo de senha em `LoginPage`/`RegisterPage` (`registerForm.ts`), `types.ts`, testes.
- Atualizar `ARCHITECTURE.md`, `GLOSSARY.md` (se citar token), README e Swagger.

## Progresso
- [x] (2026-10-14) Marco 1
- [x] (2026-10-14) Marco 2
- [x] (2026-10-14) Marco 3
- [x] (2026-10-14) Marco 4

## Descobertas e surpresas
- O `DefaultBearerTokenResolver` aceita `bearer` minúsculo (a RFC 6750 diz que o esquema não diferencia maiúsculas). O `SecurityIT.lowercaseBearer`, que esperava 401, agora espera 200.
- Sem o filtro próprio, o `MockMvcServletPath` do `IntegrationTest`, que ajustava o `servletPath` para o `shouldNotFilter`, deixou de ser necessário e foi removido.

## Registro de decisões
- 2026-10-14: JWT HS256 com o resource server do Spring, em vez de jjwt. Já integra no `SecurityFilterChain` e elimina o filtro próprio. Um segredo simétrico basta porque o próprio backend emite e valida o token.
- 2026-10-14: `password_hash` aceita nulo. Uma coluna NOT NULL quebraria a migration em bancos de dev que já têm clientes. Cliente sem senha não consegue entrar.
- 2026-10-14: uma só mensagem de 401 para documento inexistente e para senha errada, para não revelar quais documentos estão cadastrados.
- 2026-10-14: sem refresh token nem logout no servidor. O JWT expira em 8h e o logout do frontend apaga a sessão local.

## Validação e aceite
- `cd backend && sh ./mvnw verify`
- `cd frontend && npm run lint && npm test && npm run build`
- `docker compose up --build`: cadastrar com senha, entrar, errar a senha (401), enviar mensagem, reiniciar só o backend e confirmar que a sessão continua valendo.

## Resultado
Entregue: senha com BCrypt no cadastro e no login, JWT HS256 de 8h validado pelo resource server, `TokenAuthenticationFilter` removido (as rotas públicas ficam só no `SecurityConfig`), perfil `dev` padrão, CORS configurável e campos de senha no frontend. `sh ./mvnw verify`: 156 unitários e 44 ITs verdes, cobertura do JaCoCo atingida. Frontend: lint, 48 testes e build OK. Com o docker-compose: cadastro 201, senha errada 401 com a mensagem genérica, envio 201 e o mesmo token aceito depois de `docker compose restart backend`.
Ficou de fora: refresh token, revogação/logout no servidor, troca de senha e rate limit no `POST /auth` (Fase 5 do roadmap).
