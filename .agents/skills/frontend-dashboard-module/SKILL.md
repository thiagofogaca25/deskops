---
name: frontend-dashboard-module
description: >-
  Use this skill when building, styling, or updating the DeskOps React + Vite +
  Tailwind CSS frontend dashboard, including the Ticket Kanban board, Asset
  Inventory table with instant search, Asset Diagnostic view, and Ticket
  creation/detail modals.
---

# Frontend Dashboard Runbook — React + Vite + Tailwind CSS (DeskOps)

Siga este roteiro ao construir ou evoluir a interface web do **DeskOps** em `frontend/`.

## 1. Estrutura de Módulos da Interface
A interface deve oferecer navegação clara entre os 3 módulos especificados + indicadores operacionais:

1. **Resumo Operacional (KPI Cards no Topo):**
   - Total de Chamados em Aberto / Em Atendimento.
   - Chamados Críticos / com SLA em risco ou violado.
   - Ativos em Uso vs. Em Manutenção.
   - MTTR / Taxa de cumprimento de SLA.

2. **Módulo 1 — Kanban de Chamados (`ABERTO`, `EM_ATENDIMENTO`, `AGUARDANDO_USUARIO`, `AGUARDANDO_PECA`, `RESOLVIDO`, `FECHADO`):**
   - Cada card exibe:
     - `protocolo` em fonte monoespaçada (`font-mono text-xs`).
     - Badge colorido por `prioridade` (`CRITICA` em vermelho, `ALTA` em laranja, `MEDIA` em âmbar, `BAIXA` em esmeralda).
     - `titulo` e `categoria`.
     - Tag do ativo vinculado (ex: `TI-00142`), clicável para abrir o Diagnóstico do Ativo.
     - Indicador visual do prazo de SLA (`dataLimiteSla`).
   - Ação rápida para avançar/alterar status do chamado, atribuir técnico e abrir modal de detalhes com a **Linha do Tempo (Timeline)** de notas públicas e notas privadas de TI (`isPrivadaTi`).

3. **Módulo 2 — Tabela de Inventário de Ativos:**
   - Barra de busca instantânea filtrando por `tagPatrimonio`, `ipLocal`, `modelo` ou nome do `Colaborador` associado.
   - Filtros rápidos por `StatusAtivo` (`DISPONIVEL`, `EM_USO`, `EM_MANUTENCAO`, `DESCARTADO`) e `TipoAtivo`.
   - Botão "Ver Diagnóstico" em cada linha para abrir a Tela de Diagnóstico do Ativo.

4. **Módulo 3 — Tela / Painel de Diagnóstico do Ativo:**
   - Exibe o cartão técnico completo do hardware selecionado:
     - Tag Patrimonial, Fabricante, Modelo, Número de Série, Status.
     - Especificações técnicas: Processador, Memória RAM, Armazenamento, Sistema Operacional.
     - Conectividade: Endereço IP e MAC Address.
     - Colaborador responsável atual (nome, e-mail, departamento).
   - **Histórico de Falhas (Chamados Vinculados):** Lista todos os tickets já abertos para aquele ativo com status, prioridade e data.
   - **Log de Movimentação (`historico_ativos`):** Linha do tempo de auditoria patrimonial da máquina.

## 2. Verificação
1. Execute `npm run build` em `frontend/` para garantir zero erros de compilação/empacotamento.
2. Verifique responsividade e contraste dos badges de prioridade e status.
