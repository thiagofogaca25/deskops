package com.deskops.controller;

import com.deskops.domain.enums.PerfilUsuario;
import com.deskops.dto.ColaboradorRequest;
import com.deskops.dto.ColaboradorResponse;
import com.deskops.service.ColaboradorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/colaboradores")
@Tag(name = "1. Colaboradores", description = "Gestão de colaboradores solicitantes, técnicos de suporte N1/N2 e administradores de TI")
public class ColaboradorController {

    private final ColaboradorService colaboradorService;

    public ColaboradorController(ColaboradorService colaboradorService) {
        this.colaboradorService = colaboradorService;
    }

    @GetMapping
    @Operation(summary = "Listar colaboradores", description = "Retorna a lista de colaboradores cadastrados, com filtro opcional por perfil de acesso.")
    public ResponseEntity<List<ColaboradorResponse>> listar(
            @RequestParam(required = false) PerfilUsuario perfil
    ) {
        return ResponseEntity.ok(colaboradorService.listarTodos(perfil));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar colaborador por ID", description = "Consulta os dados de um colaborador específico pelo seu UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Colaborador encontrado"),
            @ApiResponse(responseCode = "404", description = "Colaborador não encontrado (RFC 7807 ProblemDetail)")
    })
    public ResponseEntity<ColaboradorResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(colaboradorService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo colaborador", description = "Registra um novo colaborador ou operador técnico garantindo unicidade de e-mail.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Colaborador cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos no payload (RFC 7807 ProblemDetail)"),
            @ApiResponse(responseCode = "409", description = "E-mail corporativo já cadastrado (RFC 7807 ProblemDetail)")
    })
    public ResponseEntity<ColaboradorResponse> criar(@Valid @RequestBody ColaboradorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(colaboradorService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar colaborador", description = "Atualiza dados cadastrais e perfil de um colaborador existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Colaborador atualizado"),
            @ApiResponse(responseCode = "404", description = "Colaborador não encontrado"),
            @ApiResponse(responseCode = "409", description = "E-mail em conflito com outro registro")
    })
    public ResponseEntity<ColaboradorResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ColaboradorRequest request
    ) {
        return ResponseEntity.ok(colaboradorService.atualizar(id, request));
    }
}
