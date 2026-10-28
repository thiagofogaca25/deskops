package com.deskops.controller;

import com.deskops.dto.DashboardMetricasResponse;
import com.deskops.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "4. Métricas Operacionais & SLA", description = "Indicadores de desempenho da operação de TI (Taxa de SLA, MTTR, Volumetria por Status e Criticidade)")
public class DashboardController {

    private final TicketService ticketService;

    public DashboardController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/metricas")
    @Operation(
            summary = "Obter indicadores operacionais de TI",
            description = "Retorna taxa de cumprimento de SLA, tempo médio de reparo (MTTR), volumetria de chamados e resumo do parque de ativos."
    )
    public ResponseEntity<DashboardMetricasResponse> obterMetricas() {
        return ResponseEntity.ok(ticketService.calcularMetricasDashboard());
    }
}
