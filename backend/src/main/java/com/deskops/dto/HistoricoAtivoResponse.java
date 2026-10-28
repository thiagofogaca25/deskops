package com.deskops.dto;

import com.deskops.domain.entity.HistoricoAtivo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Evento de histórico patrimonial de um Ativo")
public record HistoricoAtivoResponse(
        UUID id,
        UUID ativoId,
        String tagPatrimonio,
        String observacao,
        LocalDateTime dataRegistro
) {
    public static HistoricoAtivoResponse fromEntity(HistoricoAtivo h) {
        return new HistoricoAtivoResponse(
                h.getId(),
                h.getAtivo().getId(),
                h.getAtivo().getTagPatrimonio(),
                h.getObservacao(),
                h.getDataRegistro()
        );
    }
}
