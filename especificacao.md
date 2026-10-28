# Especificação Técnica e Funcional - DeskOps

O **DeskOps** é uma plataforma integrada de **Help Desk interno e Gerenciamento de Ativos de TI (ITAM - IT Asset Management)**, projetada com foco nas demandas reais de equipes de Suporte Técnico (Nível 1, 2 e Field Service) e Operações de Infraestrutura. 

Este documento detalha a proposta do sistema, escopo funcional, regras de negócio, arquitetura em Java 21 / Spring Boot 3 e especificações para a camada de visualização (Swagger UI e React Dashboard).

---

## 1. Visão Geral e Proposta de Valor

### 1.1 Contexto e Problema
Em pequenas e médias empresas ou departamentos corporativos, o suporte técnico frequentemente sofre com dois problemas centrais:
1. **Chamados descentralizados:** Solicitações via WhatsApp, Teams ou e-mail sem histórico, fila ordenada ou cálculo de tempo de resolução.
2. **Ativos invisíveis:** Máquinas, monitores, switches e impressoras sem identificação patrimonial clara, histórico de manutenções ou registro do responsável atual.

### 1.2 A Solução DeskOps
Uma solução centralizada onde cada solicitação técnica é registrada e vinculada diretamente ao patrimônio do equipamento com falha. Isso viabiliza:
* Rastreamento do ciclo de vida de equipamentos problemáticos.
* Aplicação de SLAs por nível de criticidade.
* Diagnóstico ágil de incidentes repetitivos em estações de trabalho específicas.

---

## 2. Perfis de Usuário e Acessos

| Perfil | Descrição | Permissões Principais |
| :--- | :--- | :--- |
| **Colaborador / Solicitante** | Funcionário de qualquer setor da empresa. | Abertura de chamados, visualização de suas próprias solicitações, acompanhamento de histórico e interação via comentários. |
| **Técnico de Suporte (N1/N2)** | Operador da equipe de TI. | Assumir chamados, transicionar status de atendimento, adicionar anotações técnicas, consultar e vincular ativos aos incidentes. |
| **Administrador de TI** | Gestor ou líder técnico de suporte/infra. | Cadastro de inventário, relatórios de SLA e volumetria, gestão de colaboradores e configuração de parâmetros globais. |

---

## 3. Escopo Funcional Detalhado

### 3.1 Gestão de Inventário de Ativos (Assets)
* **Cadastro Patrimonial:** Registro de ativos com os campos:
  * Tag de Patrimônio (identificador visual único, ex: `TI-00142`).
  * Categoria: `DESKTOP`, `NOTEBOOK`, `MONITOR`, `IMPRESSORA`, `SWITCH`, `ROTEADOR`, `OUTRO`.
  * Fabricante, Modelo e Número de Série.
  * Especificações Técnicas (Processador, Memória RAM, Armazenamento, SO).
  * Endereço MAC e IP Estático/Dinâmico (para diagnóstico de conectividade).
  * Status: `DISPONIVEL`, `EM_USO`, `EM_MANUTENCAO`, `DESCARTADO`.
* **Associação de Posse:** Associação 1:N entre Colaborador e Ativo, permitindo emitir termos de entrega ou rastrear a máquina em uso pelo solicitante.
* **Histórico de Falhas:** Consulta direta de todos os incidentes que já envolveram determinado hardware.

### 3.2 Gestão de Chamados (Ticketing)
* **Abertura Inteligente:**
  * Protocolo sequencial gerado automaticamente (ex: `TKT-202610-0012`).
  * Classificação por Categoria: `HARDWARE`, `SOFTWARE`, `REDE/ACESSO`, `TELEFONIA`, `DUVIDA_GERAL`.
  * Severidade/Impacto: `BAIXA`, `MEDIA`, `ALTA`, `CRITICA`.
  * Vínculo opcional ou obrigatório a um ativo cadastrado.
* **Ciclo de Vida do Chamado (State Machine):**
  1. `ABERTO`: Registrado na fila geral de triagem.
  2. `EM_ATENDIMENTO`: Assumido por um técnico.
  3. `AGUARDANDO_USUARIO`: O suporte solicitou testes ou informações adicionais.
  4. `AGUARDANDO_PECA`: Dependência de compra de suprimento ou peça de reposição.
  5. `RESOLVIDO`: Causa-raiz solucionada e validação de solução documentada.
  6. `FECHADO`: Validação final do usuário ou fechamento automático por inatividade.
* **Linha do Tempo (Timeline):** Registro cronológico de notas públicas (visíveis ao usuário) e notas internas (restritas à equipe de TI para debugar problemas).

### 3.3 Métricas Operacionais de TI
* **Controle de SLA:**
  * Cálculo dinâmico do prazo de atendimento com base na prioridade:
    * *Crítica:* 2 horas úteis.
    * *Alta:* 8 horas úteis.
    * *Média:* 24 horas úteis.
    * *Baixa:* 48 horas úteis.
* **Indicadores:** Taxa de resolução no primeiro contato (First Contact Resolution), tempo médio de resposta (MTTA) e tempo médio de reparo (MTTR).

---

## 4. Arquitetura do Sistema e Stack Tecnológica

### 4.1 Tecnologias Adotadas

```
[ Frontend: React + Vite + Tailwind ]
               │ (HTTP / JSON)
               ▼
[ Backend: Java 21 + Spring Boot 3.x ]
   ├── Spring Web & Validation (Controllers / DTOs)
   ├── Spring Data JPA & Hibernate (Persistência)
   ├── Springdoc-OpenAPI (Documentação Swagger)
   └── Spring Boot Actuator (Métricas de Saúde / Liveness / Readiness)
               │ (TCP / SQL)
               ▼
   [ Banco de Dados: PostgreSQL 16 ]
```

### 4.2 Camadas da Aplicação Backend

* **Controller Layer:** Responsável pelos contratos HTTP, serialização de payloads e documentação com anotações OpenAPI (`@Operation`, `@ApiResponse`).
* **DTO Layer (Data Transfer Objects):** Records imutáveis (`request` e `response`) para impedir que entidades do Hibernate vazem diretamente para os clientes HTTP.
* **Service Layer:** Contém as regras de negócio puras (cálculo de SLA, verificação de disponibilidade de máquina, transição válida de estados).
* **Repository Layer:** Interfaces `JpaRepository` com consultas nativas ou JPQL otimizadas para filtros e paginação.
* **Global Exception Handler:** Interceptador (`@RestControllerAdvice`) que formata todas as falhas de validação (`MethodArgumentNotValidException`) e erros de negócio em respostas no padrão **RFC 7807 (Problem Details)**.

---

## 5. Modelagem de Dados Relacional (PostgreSQL)

```
                    +--------------------+
                    |    colaboradores   |
                    +--------------------+
                    | id (UUID, PK)      |
                    | nome (VARCHAR)     |
                    | email (VARCHAR, UQ)|
                    | cargo (VARCHAR)    |
                    | departamento (VAR) |
                    +--------------------+
                              │ 1
                              │
                              │ N
+---------------------+       │       +----------------------------+
|       ativos        |       │       |          tickets           |
+---------------------+       │       +----------------------------+
| id (UUID, PK)       |       │       | id (UUID, PK)              |
| tag_patrimonio (UQ) |       │       | protocolo (VARCHAR, UQ)    |
| tipo (ENUM)         |       │       | titulo (VARCHAR)           |
| fabricante (VARCHAR)|       │       | descricao (TEXT)           |
| modelo (VARCHAR)    |       │       | categoria (ENUM)           |
| ip_local (VARCHAR)  |       │       | prioridade (ENUM)          |
| status (ENUM)       |       │       | status (ENUM)              |
| colaborador_id (FK) |◄──────┼───────| colaborador_id (FK)        |
+---------------------+ 1     │       | tecnico_atribuido_id (FK)  |
           │                  │       | ativo_id (FK, Opcional)    |
           │ 1                │       | data_abertura (TIMESTAMP)  |
           │                  │       | data_limite_sla (TIMESTAMP)|
           │ N                │       | data_fechamento (TIMESTAMP)|
           ▼                  │       +----------------------------+
+---------------------+       │                      │ 1
| historico_ativos    |       │                      │
+---------------------+       │                      │ N
| id (UUID, PK)       |       │                      ▼
| ativo_id (FK)       |       │       +----------------------------+
| observacao (TEXT)   |       │       |       ticket_notas         |
| data_registro (TS)  |       │       +----------------------------+
+---------------------+       │       | id (UUID, PK)              |
                              │       | ticket_id (FK)             |
                              └──────►| autor_id (FK)              |
                                      | conteudo (TEXT)            |
                                      | is_privada_ti (BOOLEAN)    |
                                      | data_registro (TIMESTAMP)  |
                                      +----------------------------+
```

---

## 6. Ferramentas de Visualização e Teste

### 6.1 Swagger UI (Documentação Interativa de API)
* **Objetivo:** Permitir que recrutadores e desenvolvedores executem qualquer rota sem depender de ferramentas externas (como Postman/Insomnia).
* **Configuração:** Gerada automaticamente em `/swagger-ui/index.html` via dependência `springdoc-openapi-starter-webmvc-ui`.
* **Recursos:** Exemplos reais de payloads para envio (`RequestBody`), schemas detalhados das entidades e simulações de códigos `400 Bad Request`, `404 Not Found` e `409 Conflict`.

### 6.2 Spring Boot Actuator (Painel de Saúde e Diagnóstico Operacional)
* **Objetivo:** Demonstrar competência com ferramentas de monitoramento de infraestrutura de TI.
* **Endpoints Relevantes:**
  * `GET /actuator/health`: Informa a disponibilidade da aplicação, conexão com o banco de dados e espaço livre em disco.
  * `GET /actuator/metrics`: Expõe métricas de latência HTTP e consumo de memória da JVM.

### 6.3 Dashboard Web (React + Vite + Tailwind CSS)
* **Objetivo:** Servir como portfólio visual para screenshots e demonstração no `README.md`.
* **Módulos da Interface:**
  1. **Kanban de Chamados:** Colunas interativas com badges coloridos por prioridade (`CRÍTICA` em vermelho, `ALTA` em laranja).
  2. **Tabela de Inventário:** Listagem com busca instantânea por Tag de Patrimônio, IP ou Colaborador associado.
  3. **Tela de Diagnóstico do Ativo:** Exibição do hardware selecionado com a listagem de todos os chamados já atrelados àquela máquina.

---

## 7. Roteiro de Implementação Sugerido (Passo a Passo)

1. **Sprint 1 - Fundação:**
   * Configuração do projeto via Spring Initializr (Java 21, Spring Web, Spring Data JPA, PostgreSQL Driver, Validation, Lombok/Records).
   * Arquivo `docker-compose.yml` para instanciar o PostgreSQL e a aplicação.
   * Modelagem de Entidades e Repositórios (`Colaborador` e `Ativo`).

2. **Sprint 2 - Regras de Negócio e Suporte:**
   * Implementação do módulo de `Tickets` com cálculo automático de SLA.
   * Criação do `@RestControllerAdvice` para tratamento padronizado de erros.
   * Configuração do Swagger UI com anotações descritivas.

3. **Sprint 3 - Visualização e Acabamento:**
   * Integração de endpoints do Spring Boot Actuator.
   * Interface React com tabelas e filtros de chamados/ativos.
   * Gravação de um GIF de 15 segundos demonstrando a aplicação e captura de prints para o repositório.