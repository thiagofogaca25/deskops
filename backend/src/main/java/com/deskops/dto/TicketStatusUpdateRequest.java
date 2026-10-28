package com.deskops.dto;

import com.deskops.domain.enums.StatusTicket;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Dados para transição de status e atribuição de técnico na Máquina de Estados do Chamado")
public record TicketStatusUpdateRequest(
        @Schema(description = "Novo status do chamado", example = "EM_ATENDIMENTO")
        @NotNull(message = "O novo status do chamado é obrigatório")
        StatusTicket status,

        @Schema(description = "ID do técnico responsável pelo atendimento (obrigatório ao assumir atendimento)")
        UUID tecnicoAtribuidoId,

        @Schema(description = "Nota opcional de transição para registrar na timeline do chamado", example = "Iniciado diagnóstico remoto via acesso na estação.")
        String observacaoTransicao,

        @Schema(description = "Indica se a nota de transição deve ser privada para a equipe de TI", example = "false")
        Boolean notaPrivadaTi
) {
}
