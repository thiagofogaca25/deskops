package com.deskops.dto;

import com.deskops.domain.enums.StatusAtivo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Payload para atualização de status ou posse de um ativo")
public record AtivoStatusUpdateRequest(
        @Schema(description = "Novo status do ativo", example = "EM_MANUTENCAO")
        @NotNull(message = "O novo status do ativo é obrigatório")
        StatusAtivo status,

        @Schema(description = "ID do colaborador associado (nulo para desvincular)")
        UUID colaboradorId,

        @Schema(description = "Observação para auditoria no histórico do ativo", example = "Substituição preventiva de bateria")
        String observacao
) {
}
