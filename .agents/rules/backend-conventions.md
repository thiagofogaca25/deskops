# Regras de Backend — Spring Boot 3 & Java 21 (DeskOps)

## Padrões de Código e Persistência
- Utilize recursos modernos do **Java 21**: `record` para DTOs, Pattern Matching em `switch` para cálculo de SLA e máquina de estados, e `Text Blocks` para queries JPQL multilinhas.
- Mapeamento JPA:
  - Use `@Enumerated(EnumType.STRING)` em todos os campos de enumeração (`TipoAtivo`, `StatusAtivo`, `CategoriaTicket`, `PrioridadeTicket`, `StatusTicket`, `PerfilUsuario`). Nunca use `EnumType.ORDINAL`.
  - Relacionamentos `@ManyToOne(fetch = FetchType.LAZY)` por padrão para evitar o problema de consultas N+1. Use `JOIN FETCH` ou `@EntityGraph` nos repositórios quando precisar carregar associações.
  - Datas e horários devem usar `LocalDateTime` ou `OffsetDateTime` anotados adequadamente e gerados no momento do evento.

## Padronização de Erros (RFC 7807 - Problem Details)
- Todas as exceções tratadas no `GlobalExceptionHandler` (`@RestControllerAdvice`) devem retornar `org.springframework.http.ProblemDetail`:
  - `ResourceNotFoundException` → `HttpStatus.NOT_FOUND (404)`
  - `BusinessConflictException` / violação de constraint única (`tag_patrimonio`, `email`, `protocolo`) → `HttpStatus.CONFLICT (409)`
  - `InvalidStateTransitionException` / regra de negócio inválida → `HttpStatus.UNPROCESSABLE_ENTITY (422)` ou `HttpStatus.BAD_REQUEST (400)`
  - `MethodArgumentNotValidException` → `HttpStatus.BAD_REQUEST (400)` contendo propriedade extra `errors` (mapa ou lista de `campo` e `mensagem`).

## Documentação OpenAPI (Springdoc)
- Cada Controller deve possuir `@Tag(name = "...", description = "...")`.
- Cada endpoint deve documentar `@Operation(summary = "...", description = "...")` e `@ApiResponses` contemplando os códigos `200/201`, `400`, `404` e `409`.
