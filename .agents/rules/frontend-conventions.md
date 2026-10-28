# Regras de Frontend — React + Vite + Tailwind CSS (DeskOps)

## Identidade Visual e UX Operacional (ITSM / ITAM)
- Layout focado em produtividade de equipes de Suporte N1/N2 e Infraestrutura.
- Cores semânticas padronizadas para **Prioridade de Chamado**:
  - `CRITICA`: Vermelho (`bg-red-100 text-red-800 border-red-300` / dark equivalentes)
  - `ALTA`: Laranja (`bg-orange-100 text-orange-800 border-orange-300`)
  - `MEDIA`: Azul/Âmbar (`bg-amber-100 text-amber-800 border-amber-300`)
  - `BAIXA`: Verde/Cinza (`bg-emerald-100 text-emerald-800 border-emerald-300`)
- Cores semânticas padronizadas para **Status de Ativo**:
  - `DISPONIVEL`: Verde (`emerald`)
  - `EM_USO`: Azul (`blue`)
  - `EM_MANUTENCAO`: Amarelo/Laranja (`amber`)
  - `DESCARTADO`: Cinza/Vermelho (`slate`/`rose`)

## Componentes e Integração
- Centralize chamadas HTTP em uma camada de serviço (`src/services/api.ts` ou `api.js`) apontando para a URL da API Spring Boot (`VITE_API_URL`, padrão `http://localhost:8080/api`).
- Na **Tela de Diagnóstico do Ativo**, exiba:
  1. Cabeçalho com `tagPatrimonio`, `tipo`, `fabricante`, `modelo`, `status` e `ipLocal`/`macAddress`.
  2. Especificações de hardware (CPU, RAM, Armazenamento, SO) e Colaborador responsável atual.
  3. Lista cronológica de todos os chamados vinculados àquele ativo e o log de `historico_ativos`.
