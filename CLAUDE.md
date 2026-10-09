# CLAUDE.md

## Banco de dados (Flyway)

- O schema é versionado com Flyway em `backend/src/main/resources/db/migrations`. O Hibernate roda com `ddl-auto=validate` e não cria nem altera tabelas.
- Toda entidade nova, tabela nova ou mudança em entidade existente (coluna, tipo, constraint, índice, valor de enum) exige uma nova migration `V<n>__<descricao_em_snake_case>.sql`, com `n` seguinte à última versão existente.
- Nunca editar uma migration já criada; corrigir com uma nova versão.
- A migration precisa bater com o mapeamento JPA (nomes, tipos, nullability, tamanhos), senão o `validate` derruba a aplicação. Enums `@Enumerated(STRING)` levam `CHECK` com os valores aceitos.
