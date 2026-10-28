# DeskOps — Project Rules & Architecture Guidelines

O **DeskOps** é uma plataforma integrada de **Help Desk interno e Gerenciamento de Ativos de TI (ITAM)** para equipes de Suporte Técnico (N1, N2 e Field Service) e Operações de Infraestrutura.

Sempre siga estas diretrizes arquiteturais, de domínio e de código ao criar ou modificar arquivos neste repositório.

---

## 1. Estrutura do Repositório

- `especificacao.md`: Documento fonte da verdade para escopo funcional, modelagem e roteiro.
- `docker-compose.yml`: Orquestração do PostgreSQL 16 e serviços da aplicação.
- `backend/`: Aplicação Java 21 + Spring Boot 3.x.
- `frontend/`: Aplicação React + Vite + Tailwind CSS.

---

## 2. Regras de Arquitetura Backend (Java 21 + Spring Boot 3.x)

1. **Separação Estrita de Camadas:**
   - `controller`: Contratos REST, validação de entrada (`@Valid`) e documentação OpenAPI (`@Tag`, `@Operation`, `@ApiResponses`). **Proibido** conter regras de negócio ou acessar repositórios diretamente.
   - `dto`: Exclusivamente **Java Records** imutáveis (`*Request`, `*Response`). Entidades JPA **nunca** devem ser expostas diretamente nos endpoints HTTP.
   - `service`: Regras de negócio puras (cálculo de SLA, geração de protocolo, máquina de estados do chamado, associação de posse de ativo e registro automático de histórico).
   - `domain` / `entity` & `enums`: Entidades JPA mapeadas para o PostgreSQL 16 usando `UUID` como chave primária (`@Id @GeneratedValue(strategy = GenerationType.UUID)`).
   - `repository`: Interfaces `JpaRepository<Entity, UUID>` com queries JPQL otimizadas para filtros combinados e paginação.
   - `exception`: Tratamento global via `@RestControllerAdvice` retornando exclusivamente o padrão **RFC 7807 (`org.springframework.http.ProblemDetail`)** para `400 Bad Request`, `404 Not Found`, `409 Conflict` e `422 Unprocessable Entity`.

2. **Documentação Interativa (Swagger UI) & Observabilidade (Actuator):**
   - Todo DTO e Controller deve incluir metadados `@Schema(example = "...")` com exemplos reais do domínio de TI (ex: tag `TI-00142`, protocolo `TKT-202610-0012`, IP `192.168.10.45`).
   - Os endpoints `/swagger-ui/index.html`, `/actuator/health` e `/actuator/metrics` devem estar sempre funcionais e configurados no `application.yml`.

---

## 3. Regras de Domínio e Modelagem de Dados

### 3.1 Tabelas e Chaves
- Todas as 5 tabelas principais (`colaboradores`, `ativos`, `historico_ativos`, `tickets`, `ticket_notas`) utilizam `UUID` como PK.
- Constraints de unicidade obrigatórias:
  - `colaboradores.email` (`UNIQUE`)
  - `ativos.tag_patrimonio` (`UNIQUE`, formato padrão `TI-XXXXX`)
  - `tickets.protocolo` (`UNIQUE`, formato padrão `TKT-YYYYMM-XXXX`)

### 3.2 Gestão de Ativos (ITAM)
- **Categorias (`TipoAtivo`):** `DESKTOP`, `NOTEBOOK`, `MONITOR`, `IMPRESSORA`, `SWITCH`, `ROTEADOR`, `OUTRO`.
- **Status (`StatusAtivo`):** `DISPONIVEL`, `EM_USO`, `EM_MANUTENCAO`, `DESCARTADO`.
- **Invariantes de Ativo:**
  - Um ativo `DESCARTADO` não pode ser associado a um colaborador nem vinculado a novos chamados.
  - Sempre que um ativo trocar de colaborador responsável, mudar de status ou for vinculado a um incidente de hardware, um registro em `historico_ativos` deve ser gravado automaticamente.

### 3.3 Gestão de Chamados (Ticketing) & SLA
- **Categorias (`CategoriaTicket`):** `HARDWARE`, `SOFTWARE`, `REDE_ACESSO`, `TELEFONIA`, `DUVIDA_GERAL`.
- **Prioridades (`PrioridadeTicket`) e Cálculo de SLA (`data_limite_sla`):**
  - `CRITICA`: +2 horas úteis a partir de `data_abertura`.
  - `ALTA`: +8 horas úteis a partir de `data_abertura`.
  - `MEDIA`: +24 horas úteis a partir de `data_abertura`.
  - `BAIXA`: +48 horas úteis a partir de `data_abertura`.
- **Máquina de Estados (`StatusTicket`):**
  - Estados: `ABERTO`, `EM_ATENDIMENTO`, `AGUARDANDO_USUARIO`, `AGUARDANDO_PECA`, `RESOLVIDO`, `FECHADO`.
  - Chamados criados iniciam sempre em `ABERTO`.
  - Transicionar para `EM_ATENDIMENTO` exige um técnico atribuído (`tecnico_atribuido_id`).
  - Chamados em `FECHADO` são imutáveis (não aceitam novas transições de status nem novas notas).
- **Notas (`ticket_notas`):**
  - Possuem flag `is_privada_ti` (`boolean`). Notas privadas são visíveis apenas para perfis de TI (`TECNICO`, `ADMIN`), enquanto notas públicas compõem a timeline visível ao colaborador solicitante.

---

## 4. Regras de Frontend (React + Vite + Tailwind CSS)

- **Três Módulos Obrigatórios no Dashboard:**
  1. **Kanban de Chamados:** Colunas por `StatusTicket`, cards exibindo protocolo, título, ativo vinculado, indicador de SLA e badges coloridos por prioridade (`CRITICA` em vermelho, `ALTA` em laranja, `MEDIA` em amarelo/azul, `BAIXA` em cinza/verde).
  2. **Tabela de Inventário de Ativos:** Listagem com busca instantânea por `tag_patrimonio`, `ip_local` ou nome do `Colaborador` associado, além de filtro por status/tipo.
  3. **Tela/Painel de Diagnóstico do Ativo:** Exibe especificações de hardware (CPU, RAM, Armazenamento, SO, MAC, IP), histórico patrimonial e todos os chamados já atrelados àquela máquina.
