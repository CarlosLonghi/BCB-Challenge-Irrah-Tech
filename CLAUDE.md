# CLAUDE.md

## Docs para agents

Leia sob demanda, não precisa carregar tudo em toda tarefa:

- [`.claude/ARCHITECTURE.md`](.claude/ARCHITECTURE.md): mapa de pacotes, fluxos (auth, envio, fila, worker), invariantes e testes. Leia antes de mexer numa parte do código que você não conhece ou de criar classe/pasta nova.
- [`.claude/GLOSSARY.md`](.claude/GLOSSARY.md): termos do domínio ↔ nomes no código. Consulte quando um termo ou status não estiver claro.
- [`.claude/PLANS.md`](.claude/PLANS.md): como escrever um ExecPlan. Use antes de tarefa grande (várias etapas, backend + frontend, schema/fila/cobrança). Os planos ficam em [`.claude/execplans/`](.claude/execplans/README.md).

Se uma mudança deixar algum desses arquivos desatualizado, atualize-o no mesmo PR.

## Banco de dados (Flyway)

- O schema é versionado com Flyway em `backend/src/main/resources/db/migrations`. O Hibernate roda com `ddl-auto=validate` e não cria nem altera tabelas.
- Toda entidade nova, tabela nova ou mudança em entidade existente (coluna, tipo, constraint, índice, valor de enum) exige uma nova migration `V<n>__<descricao_em_snake_case>.sql`, com `n` seguinte à última versão existente.
- Nunca editar uma migration já criada; corrigir com uma nova versão.
- A migration precisa bater com o mapeamento JPA (nomes, tipos, nullability, tamanhos), senão o `validate` derruba a aplicação. Enums `@Enumerated(STRING)` levam `CHECK` com os valores aceitos.
