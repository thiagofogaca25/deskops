---
name: ticket-sla-and-state-machine
description: >-
  Use this skill when implementing or modifying DeskOps ticketing business rules,
  including sequential protocol generation (TKT-YYYYMM-XXXX), SLA deadline
  calculation by priority, ticket state machine transitions, timeline notes
  (public vs internal IT), and operational metrics (MTTA, MTTR, SLA compliance).
---

# Ticket SLA & State Machine Runbook (DeskOps)

Use este guia ao implementar ou alterar as regras de negócio de chamados (`Ticket` e `TicketNota`) e métricas de SLA no **DeskOps**.

## 1. Geração Automática de Protocolo (`TKT-YYYYMM-XXXX`)
1. Ao abrir um chamado, gere o prefixo baseado no ano e mês correntes: `TKT-` + `YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"))` + `-`.
2. Consulte a contagem ou o último sequencial do mês no `TicketRepository` e formate com 4 dígitos (`String.format("%04d", proximoNumero)`), ex: `TKT-202610-0012`.

## 2. Cálculo Automático de SLA (`dataLimiteSla`)
Utilize Pattern Matching em `switch` do Java 21 sobre `PrioridadeTicket`:
```java
LocalDateTime dataLimiteSla = switch (prioridade) {
    case CRITICA -> dataAbertura.plusHours(2);
    case ALTA    -> dataAbertura.plusHours(8);
    case MEDIA   -> dataAbertura.plusHours(24);
    case BAIXA   -> dataAbertura.plusHours(48);
};
```

## 3. Validação da Máquina de Estados (`StatusTicket`)
1. Valide toda transição de status através de um método dedicado `validarTransicao(StatusTicket atual, StatusTicket novo)`:
   - De `ABERTO` para: `EM_ATENDIMENTO`, `FECHADO`.
   - De `EM_ATENDIMENTO` para: `AGUARDANDO_USUARIO`, `AGUARDANDO_PECA`, `RESOLVIDO`.
   - De `AGUARDANDO_USUARIO` para: `EM_ATENDIMENTO`, `RESOLVIDO`, `FECHADO`.
   - De `AGUARDANDO_PECA` para: `EM_ATENDIMENTO`, `RESOLVIDO`.
   - De `RESOLVIDO` para: `FECHADO`, `EM_ATENDIMENTO`.
   - De `FECHADO` para: nenhum (lance exceção de regra de negócio `422 Unprocessable Entity` / `400 Bad Request`).
2. Exija `tecnicoAtribuido` não nulo ao entrar em `EM_ATENDIMENTO`.
3. Se `novo == RESOLVIDO || novo == FECHADO`, defina `dataFechamento = LocalDateTime.now()` (se ainda nulo).
4. Se o chamado tiver um `Ativo` vinculado e categoria `HARDWARE`:
   - Ao entrar em `AGUARDANDO_PECA`, atualize o status do ativo para `EM_MANUTENCAO` e grave em `historico_ativos`.
   - Ao entrar em `RESOLVIDO` ou `FECHADO`, restaure o status do ativo para `EM_USO` (se tiver colaborador) ou `DISPONIVEL` e grave em `historico_ativos`.

## 4. Linha do Tempo (`TicketNota`)
1. Notas possuem `isPrivadaTi` (`boolean`).
2. Na listagem de notas de um chamado, permita filtrar se notas internas de TI devem ser exibidas ou ocultadas (para visão do Colaborador Solicitante).

## 5. Indicadores Operacionais (Dashboard / Relatórios)
- **SLA Compliance (%):** Percentual de chamados cujo atendimento/resolução ocorreu antes de `dataLimiteSla`.
- **MTTR (Mean Time To Repair):** Média de tempo (em horas/minutos) entre `dataAbertura` e `dataFechamento` dos chamados resolvidos/fechados.
- **Volumetria:** Contagem agrupada por `StatusTicket`, `PrioridadeTicket` e `CategoriaTicket`.
