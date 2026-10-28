package com.deskops.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Visão consolidada de Diagnóstico de um Ativo (Hardware, Histórico Patrimonial e Chamados vinculados)")
public record AtivoDiagnosticoResponse(
        AtivoResponse ativo,
        int totalIncidenteHardware,
        List<TicketResponse> chamadosVinculados,
        List<HistoricoAtivoResponse> historicoPatrimonial
) {
}
