package com.deskops.dto;

import com.deskops.domain.enums.CategoriaTicket;
import com.deskops.domain.enums.PrioridadeTicket;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Dados de entrada para abertura de um novo chamado técnico")
public record TicketRequest(
        @Schema(description = "Título resumido do incidente ou solicitação", example = "Falha de vídeo intermitente e superaquecimento na dock station")
        @NotBlank(message = "O título do chamado é obrigatório")
        String titulo,

        @Schema(description = "Descrição detalhada do problema relatado", example = "Equipamento apresenta tela preta ao conectar dois monitores HDMI após 20 minutos de uso.")
        @NotBlank(message = "ADescrição do chamado é obrigatória")
        String descricao,

        @Schema(description = "Categoria técnica do chamado", example = "HARDWARE")
        @NotNull(message = "A categoria do chamado é obrigatória")
        CategoriaTicket categoria,

        @Schema(description = "Severidade / Impacto para cálculo automático de SLA", example = "ALTA")
        @NotNull(message = "A prioridade do chamado é obrigatória")
        PrioridadeTicket prioridade,

        @Schema(description = "ID do colaborador solicitante")
        @NotNull(message = "O ID do colaborador solicitante é obrigatório")
        UUID colaboradorId,

        @Schema(description = "ID do ativo de TI vinculado ao incidente (opcional)")
        UUID ativoId,

        @Schema(description = "ID do técnico atribuído inicialmente (opcional)")
        UUID tecnicoAtribuidoId
) {
}
