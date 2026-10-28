package com.deskops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Dados para inclusão de uma anotação na Linha do Tempo (Timeline) do chamado")
public record TicketNotaRequest(
        @Schema(description = "ID do autor da nota (Colaborador ou Técnico)")
        @NotNull(message = "O autor da nota é obrigatório")
        UUID autorId,

        @Schema(description = "Conteúdo da anotação pública ou técnica", example = "Realizado teste de memória RAM com diagnóstico Dell e identificado módulo com bad block.")
        @NotBlank(message = "O conteúdo da nota é obrigatório")
        String conteudo,

        @Schema(description = "Define se a nota é restrita à equipe interna de TI (true) ou pública ao solicitante (false)", example = "true")
        boolean isPrivadaTi
) {
}
