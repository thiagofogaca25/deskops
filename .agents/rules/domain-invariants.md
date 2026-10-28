# Regras de Domínio — Máquina de Estados, SLA e Ativos (DeskOps)

## 1. Matriz de Transição de Status do Chamado (`StatusTicket`)
As transições permitidas para um `Ticket` são estritas:
- `ABERTO` ➔ `EM_ATENDIMENTO` (exige `tecnicoAtribuido` preenchido) ou `FECHADO` (cancelamento pelo solicitante).
- `EM_ATENDIMENTO` ➔ `AGUARDANDO_USUARIO`, `AGUARDANDO_PECA`, `RESOLVIDO`.
- `AGUARDANDO_USUARIO` ➔ `EM_ATENDIMENTO`, `RESOLVIDO`, `FECHADO`.
- `AGUARDANDO_PECA` ➔ `EM_ATENDIMENTO`, `RESOLVIDO`.
- `RESOLVIDO` ➔ `FECHADO` (confirmação da solução) ou `EM_ATENDIMENTO` (reabertura caso o problema persista).
- `FECHADO` ➔ *(Estado Terminal — nenhuma transição permitida)*.

Quando o ticket transiciona para `RESOLVIDO` ou `FECHADO`, o campo `dataFechamento` deve ser preenchido caso ainda esteja nulo (e limpo caso seja reaberto de `RESOLVIDO` para `EM_ATENDIMENTO`).

## 2. Cálculo de SLA por Prioridade (`PrioridadeTicket`)
Na abertura do chamado (`dataAbertura`), `dataLimiteSla` é calculada automaticamente:
- `CRITICA` → `dataAbertura.plusHours(2)`
- `ALTA` → `dataAbertura.plusHours(8)`
- `MEDIA` → `dataAbertura.plusHours(24)`
- `BAIXA` → `dataAbertura.plusHours(48)`

Um chamado é considerado **SLA Violado (`slaEstourado = true`)** se:
- Ainda não está em `RESOLVIDO`/`FECHADO` e `LocalDateTime.now().isAfter(dataLimiteSla)`, OU
- Foi concluído com `dataFechamento.isAfter(dataLimiteSla)`.

## 3. Rastreabilidade de Ativos (`Ativo` e `HistoricoAtivo`)
- Quando um chamado da categoria `HARDWARE` com ativo vinculado entra em `EM_ATENDIMENTO` ou `AGUARDANDO_PECA`, registre automaticamente um evento em `historico_ativos` descrevendo o protocolo do chamado.
- Quando o ativo tem seu `colaborador` ou `status` alterado, um registro em `historico_ativos` também é gerado automaticamente.
