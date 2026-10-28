package com.deskops.controller;

import com.deskops.domain.enums.StatusAtivo;
import com.deskops.domain.enums.TipoAtivo;
import com.deskops.dto.*;
import com.deskops.service.AtivoService;
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
@RequestMapping("/api/ativos")
@Tag(name = "2. Ativos de TI (ITAM)", description = "Gestão de inventário patrimonial, especificações de hardware, histórico e diagnóstico de máquinas")
public class AtivoController {

    private final AtivoService ativoService;

    public AtivoController(AtivoService ativoService) {
        this.ativoService = ativoService;
    }

    @GetMapping
    @Operation(
            summary = "Listar e filtrar ativos de TI",
            description = "Busca instantânea por Tag de Patrimônio (ex: TI-00142), IP local, modelo ou nome do colaborador, com filtro por status e tipo."
    )
    public ResponseEntity<List<AtivoResponse>> listar(
            @RequestParam(required = false) StatusAtivo status,
            @RequestParam(required = false) TipoAtivo tipo,
            @RequestParam(required = false) String busca
    ) {
        return ResponseEntity.ok(ativoService.listarComFiltros(status, tipo, busca));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ativo por ID", description = "Retorna as especificações técnicas e posse atual de um ativo de TI.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ativo encontrado"),
            @ApiResponse(responseCode = "404", description = "Ativo não encontrado (RFC 7807 ProblemDetail)")
    })
    public ResponseEntity<AtivoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ativoService.buscarPorId(id));
    }

    @GetMapping("/{id}/diagnostico")
    @Operation(
            summary = "Obter diagnóstico completo do ativo",
            description = "Retorna hardware detalhado, todos os chamados já abertos para a máquina e o histórico patrimonial completo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Diagnóstico retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ativo não encontrado")
    })
    public ResponseEntity<AtivoDiagnosticoResponse> obterDiagnostico(@PathVariable UUID id) {
        return ResponseEntity.ok(ativoService.obterDiagnostico(id));
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Listar histórico patrimonial do ativo", description = "Retorna a linha do tempo de movimentações e manutenções do ativo.")
    public ResponseEntity<List<HistoricoAtivoResponse>> listarHistorico(@PathVariable UUID id) {
        return ResponseEntity.ok(ativoService.listarHistorico(id));
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo ativo no inventário", description = "Registra um equipamento com Tag Patrimonial única (TI-XXXXX) e gera log inicial no histórico.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ativo cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação (ex: formato de tag inválido)"),
            @ApiResponse(responseCode = "409", description = "Tag de patrimônio já existente"),
            @ApiResponse(responseCode = "422", description = "Violação de regra de negócio (ex: ativo descartado associado a colaborador)")
    })
    public ResponseEntity<AtivoResponse> criar(@Valid @RequestBody AtivoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ativoService.criar(request));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Atualizar status operacional ou posse do ativo",
            description = "Altera status (DISPONIVEL, EM_USO, EM_MANUTENCAO, DESCARTADO) e/ou colaborador responsável, gravando auditoria automática em historico_ativos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status/posse atualizados com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ativo ou colaborador não encontrado"),
            @ApiResponse(responseCode = "422", description = "Violação de invariante do ativo")
    })
    public ResponseEntity<AtivoResponse> atualizarStatusOuPosse(
            @PathVariable UUID id,
            @Valid @RequestBody AtivoStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ativoService.atualizarStatusOuPosse(id, request));
    }
}
