# ExecPlans

Um ExecPlan é um documento de planejamento que um agent (ou uma pessoa) segue para entregar uma mudança grande do começo ao fim. Ele vive em [`execplans/`](execplans/README.md) e é atualizado durante a execução, não só escrito no início.

## Quando usar

Escreva um ExecPlan antes de começar quando a tarefa:
- tem várias etapas ou vai durar mais de uma sessão;
- atravessa backend e frontend (ex.: mudança de contrato da API);
- mexe em schema, fila, cobrança ou concorrência (ver invariantes em [ARCHITECTURE.md](ARCHITECTURE.md#invariantes-não-quebrar));
- tem decisões de design em aberto que vale registrar.

Correção pontual, ajuste de texto ou mudança em um arquivo não precisa.

## Regras

1. **Autocontido.** Quem não viu a conversa tem que conseguir executar só com o plano e o repositório. Explique o contexto, cite arquivos e classes pelo caminho, defina termos novos (ou aponte para o [GLOSSARY.md](GLOSSARY.md)).
2. **Documento vivo.** Atualize `Progresso`, `Descobertas` e `Decisões` a cada etapa concluída ou quando o plano mudar. Ao retomar, leia o plano inteiro antes de continuar.
3. **Aceite observável.** Cada marco termina com algo verificável: comando + resultado esperado (ex.: `sh ./mvnw verify` verde, `curl` com o status e o corpo esperados, comportamento na tela).
4. **Passos seguros de repetir.** Prefira passos que dá para rodar de novo sem estrago; se um passo é destrutivo ou irreversível, diga isso e como voltar.
5. **Decisão com motivo.** Toda escolha não óbvia vai para o `Registro de decisões` com o porquê e a data.
6. **Respeite o CLAUDE.md.** Migration nova para qualquer mudança de schema, commits de uma linha no padrão semântico, trabalho em branch própria e entrega por PR (a `main` é protegida).

## Fluxo

1. Crie `execplans/AAAA-MM-DD-<slug>.md` a partir do esqueleto abaixo e adicione em "Ativos" no [índice](execplans/README.md).
2. Crie a branch (`feat/<slug>`, `fix/<slug>`, `docs/<slug>`...).
3. Execute marco a marco, atualizando o plano. Rode os testes do que mudou em cada marco.
4. Ao terminar, preencha `Resultado` e mova o plano para "Concluídos" no índice. O plano entra no mesmo PR da mudança.

## Esqueleto

```markdown
# <Título curto, no imperativo>

- Status: ativo | concluído | abandonado
- Criado em: AAAA-MM-DD · Branch: `<branch>`

## Objetivo e contexto
O que muda para o usuário (ou para o sistema) e por quê. Uma ou duas frases sobre o resultado esperado.

## Estado atual
Como o código funciona hoje na parte afetada, com caminhos de arquivo e classes. O que já existe e pode ser reaproveitado.

## Plano de trabalho
### Marco 1 — <nome>
- O que fazer, em quais arquivos.
- Aceite: <comando> → <resultado esperado>.

### Marco 2 — <nome>
...

## Progresso
- [ ] Marco 1
- [ ] Marco 2
(marque com data: `- [x] (AAAA-MM-DD) Marco 1`)

## Descobertas e surpresas
Comportamentos inesperados, bugs encontrados no caminho, premissas que caíram. Com evidência curta (saída de teste, trecho de log).

## Registro de decisões
- AAAA-MM-DD — Decisão. Motivo. Alternativa descartada.

## Validação e aceite
Como provar que está pronto de ponta a ponta:
- `cd backend && sh ./mvnw verify`
- `cd frontend && npm run lint && npm test && npm run build`
- Teste manual com `docker compose up --build`: <passos>.

## Resultado
O que foi entregue, o que ficou de fora e o que aprendemos (preencher ao concluir).
```
