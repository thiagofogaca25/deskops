---
name: spring-crud-vertical-slice
description: >-
  Use this skill when creating or extending a backend module (Colaborador, Ativo,
  HistoricoAtivo, Ticket, or TicketNota) in the DeskOps Spring Boot 3 / Java 21
  application, including JPA entities, repositories, immutable Record DTOs,
  services, RFC 7807 error handling, and Swagger/OpenAPI controllers.
---

# Spring Boot 3 Vertical Slice Runbook (DeskOps)

Siga este roteiro passo a passo ao implementar ou estender um recurso vertical no backend do **DeskOps**.

## 1. Entidade JPA e Enumerações (`entity` & `enums`)
1. Defina as enumerações necessárias e anote os campos na entidade com `@Enumerated(EnumType.STRING)`.
2. Configure a chave primária como `UUID`:
   ```java
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;
   ```
3. Configure constraints de unicidade (`@Column(unique = true, nullable = false)`) para:
   - `Colaborador.email`
   - `Ativo.tagPatrimonio`
   - `Ticket.protocolo`
4. Configure relacionamentos `@ManyToOne(fetch = FetchType.LAZY)` para evitar consultas N+1.

## 2. Repositório (`repository`)
1. Crie a interface estendendo `JpaRepository<Entidade, UUID>`.
2. Adicione métodos de verificação de duplicidade (`existsByEmail`, `existsByTagPatrimonio`, etc.) e queries com `@Query` / `JOIN FETCH` para listagens filtradas.

## 3. DTOs como Java Records (`dto`)
1. Crie `<Recurso>Request` e `<Recurso>Response` como `record`.
2. Adicione validações do Jakarta Validation (`@NotBlank`, `@NotNull`, `@Email`, `@Pattern`) nos records de entrada.
3. Adicione anotações `@Schema(description = "...", example = "...")` do Springdoc OpenAPI com exemplos realistas do domínio de Suporte de TI.

## 4. Camada de Serviço (`service`)
1. Anote com `@Service` e use `@Transactional` para métodos de escrita e `@Transactional(readOnly = true)` para consultas.
2. Valide regras de unicidade antes de salvar, lançando `BusinessConflictException` (mapeada para HTTP `409 Conflict` no `ProblemDetail`).
3. Busque entidades por ID lançando `ResourceNotFoundException` (mapeada para HTTP `404 Not Found` no `ProblemDetail`).

## 5. Controller e Documentação Swagger (`controller`)
1. Anote com `@RestController`, `@RequestMapping("/api/...")` e `@Tag(name = "...", description = "...")`.
2. Documente cada rota com `@Operation` e `@ApiResponses` (`200`/`201`, `400`, `404`, `409`).
3. Use `@Valid @RequestBody` nas requisições `POST`, `PUT` e `PATCH`.

## 6. Verificação
1. Execute `./mvnw test` ou `mvn compile` dentro de `backend/`.
2. Verifique se o schema OpenAPI em `/v3/api-docs` e `/swagger-ui/index.html` renderiza o novo controller sem erros.
