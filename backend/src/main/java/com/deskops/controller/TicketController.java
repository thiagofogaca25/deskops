package com.deskops.controller;

import com.deskops.domain.enums.CategoriaTicket;
import com.deskops.domain.enums.PrioridadeTicket;
import com.deskops.domain.enums.StatusTicket;
import com.deskops.dto.*;
import com.deskops.service.TicketService;
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
@RequestMapping("/api/tickets")
@Tag(name = "3. Chamados (Ticketing & SLA)", description = "Abertura inteligente de chamados, cálculo automático de SLA, máquina de estados e timeline de notas")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    @Operation(
            summary = "Listar chamados com filtros",
            description = "Retorna chamados ordenados por data de abertura com filtros opcionais por status, prioridade, categoria e ativo vinculado."
    )
    public ResponseEntity<List<TicketResponse>> listar(
            @RequestParam(required = false) StatusTicket status,
            @RequestParam(required = false) PrioridadeTicket prioridade,
            @RequestParam(required = false) CategoriaTicket categoria,
            @RequestParam(required = false) UUID ativoId
    ) {
        return ResponseEntity.ok(ticketService.listarComFiltros(status, prioridade, categoria, ativoId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar chamado por ID", description = "Retorna todos os detalhes do chamado, prazo de SLA e vínculos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chamado encontrado"),
            @ApiResponse(responseCode = "404", description = "Chamado não encontrado (RFC 7807 ProblemDetail)")
    })
    public ResponseEntity<TicketResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.buscarPorId(id));
    }

    @PostMapping
    @Operation(
            summary = "Abrir novo chamado técnico",
            description = "Gera protocolo sequencial automático (TKT-YYYYMM-XXXX), calcula data_limite_sla por prioridade (CRITICA 2h, ALTA 8h, MEDIA 24h, BAIXA 48h) e vincula ao histórico do ativo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Chamado aberto com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação de campos"),
            @ApiResponse(responseCode = "404", description = "Colaborador ou ativo não encontrado"),
            @ApiResponse(responseCode = "422", description = "Regra de negócio violada (ex: ativo descartado)")
    })
    public ResponseEntity<TicketResponse> abrirChamado(@Valid @RequestBody TicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.abrirChamado(request));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Transicionar status do chamado (State Machine)",
            description = "Valida e executa a transição de estado (ABERTO -> EM_ATENDIMENTO -> AGUARDANDO_USUARIO/PECA -> RESOLVIDO -> FECHADO), atualizando status de manutenção do ativo quando aplicável."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transição realizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Chamado ou técnico não encontrado"),
            @ApiResponse(responseCode = "422", description = "Transição inválida na máquina de estados ou falta de técnico atribuído")
    })
    public ResponseEntity<TicketResponse> atualizarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TicketStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ticketService.atualizarStatus(id, request));
    }

    @GetMapping("/{id}/notas")
    @Operation(
            summary = "Listar notas da Linha do Tempo (Timeline) do chamado",
            description = "Retorna as anotações cronológicas do chamado. Permite ocultar notas internas de TI (incluirPrivadas=false) para visualização do solicitante."
    )
    public ResponseEntity<List<TicketNotaResponse>> listarNotas(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean incluirPrivadas
    ) {
        return ResponseEntity.ok(ticketService.listarNotas(id, incluirPrivadas));
    }

    @PostMapping("/{id}/notas")
    @Operation(
            summary = "Adicionar nota na Linha do Tempo (Timeline)",
            description = "Registra um comentário público (visível ao solicitante) ou nota técnica interna (restrita à equipe de TI)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nota adicionada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Chamado ou autor não encontrado"),
            @ApiResponse(responseCode = "422", description = "Chamado fechado não permite novas notas")
    })
    public ResponseEntity<TicketNotaResponse> adicionarNota(
            @PathVariable UUID id,
            @Valid @RequestBody TicketNotaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.adicionarNota(id, request));
    }
}
