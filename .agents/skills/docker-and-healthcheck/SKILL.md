---
name: docker-and-healthcheck
description: >-
  Use this skill when configuring or running the Docker Compose environment
  (PostgreSQL 16, Spring Boot backend, React frontend), configuring Spring Boot
  Actuator health/metrics endpoints, or seeding initial demonstration data for
  DeskOps.
---

# Docker Compose, Actuator & Seed Data Runbook (DeskOps)

Use este procedimento para configurar a infraestrutura local, validar saúde da aplicação via Spring Boot Actuator e popular dados de demonstração realistas.

## 1. Orquestração via `docker-compose.yml`
1. Certifique-se de que o serviço `postgres` utiliza a imagem `postgres:16-alpine` com:
   - Banco: `deskops_db`
   - Healthcheck via `pg_isready -U deskops -d deskops_db`.
   - Volume persistente para dados.
2. Configure o backend Spring Boot para ler variáveis de ambiente (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`) com valores default compatíveis com execução local (`localhost:5432`).

## 2. Configuração do Spring Boot Actuator e Swagger UI
No `application.yml`, garanta a exposição dos endpoints operacionais:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

## 3. Carga de Dados de Demonstração (Data Seeder)
Para permitir testes imediatos no Swagger UI e screenshots do Dashboard React:
1. Crie um `DataInitializer` (`CommandLineRunner` condicional, executado apenas quando `colaboradorRepository.count() == 0`).
2. Popule dados realistas de TI:
   - **Colaboradores:** Solicitantes de setores variados (Financeiro, RH, Comercial) e Técnicos N1/N2 + Admin de TI.
   - **Ativos:** Notebooks (`TI-00101`), Desktops (`TI-00102`), Impressoras (`TI-00201`), Switches (`TI-00301`) com especificações completas (CPU, RAM, SSD, SO, IP, MAC) e histórico patrimonial inicial.
   - **Tickets:** Chamados distribuídos em todas as colunas do Kanban (`ABERTO`, `EM_ATENDIMENTO`, `AGUARDANDO_USUARIO`, `AGUARDANDO_PECA`, `RESOLVIDO`, `FECHADO`) e prioridades (`CRITICA`, `ALTA`, `MEDIA`, `BAIXA`), incluindo notas públicas e notas internas de TI.

## 4. Verificação de Saúde
Execute os checks após subir o ambiente:
- `curl -s http://localhost:8080/actuator/health` → deve retornar `"status":"UP"` com detalhes de `db` (PostgreSQL) e `diskSpace`.
- `curl -s http://localhost:8080/actuator/metrics` → deve listar as métricas da JVM e HTTP.
- `curl -s http://localhost:8080/v3/api-docs` → deve retornar a especificação OpenAPI 3.0 completa.
