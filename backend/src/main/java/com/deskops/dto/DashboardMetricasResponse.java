package com.deskops.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Indicadores Operacionais de TI (SLA, MTTR, Volumetria de Chamados e Inventário)")
public record DashboardMetricasResponse(
        long totalTickets,
        long ticketsAbertos,
        long ticketsEmAtendimento,
        long ticketsAguardando,
        long ticketsResolvidosOuFechados,
        long ticketsCriticosAtivos,
        long ticketsSlaViolado,
        double taxaCumprimentoSlaPercentual,
        double mttrHorasMedia,
        long totalAtivos,
        long ativosEmUso,
        long ativosDisponiveis,
        long ativosEmManutencao,
        Map<String, Long> ticketsPorStatus,
        Map<String, Long> ticketsPorPrioridade,
        Map<String, Long> ticketsPorCategoria
) {
}
