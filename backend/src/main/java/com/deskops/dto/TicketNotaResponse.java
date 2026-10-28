package com.deskops.dto;

import com.deskops.domain.entity.TicketNota;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Nota registrada na Linha do Tempo (Timeline) de um chamado")
public record TicketNotaResponse(
        UUID id,
        UUID ticketId,
        ColaboradorResponse autor,
        String conteudo,
        boolean isPrivadaTi,
        LocalDateTime dataRegistro
) {
    public static TicketNotaResponse fromEntity(TicketNota n) {
        return new TicketNotaResponse(
                n.getId(),
                n.getTicket().getId(),
                ColaboradorResponse.fromEntity(n.getAutor()),
                n.getConteudo(),
                n.isPrivadaTi(),
                n.getDataRegistro()
        );
    }
}
