package com.deskops.dto;

import com.deskops.domain.entity.Ticket;
import com.deskops.domain.enums.CategoriaTicket;
import com.deskops.domain.enums.PrioridadeTicket;
import com.deskops.domain.enums.StatusTicket;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Representação completa de um Chamado (Ticket) com indicadores de SLA")
public record TicketResponse(
        UUID id,
        @Schema(description = "Protocolo sequencial gerado automaticamente", example = "TKT-202610-0012")
        String protocolo,
        String titulo,
        String descricao,
        CategoriaTicket categoria,
        PrioridadeTicket prioridade,
        StatusTicket status,
        ColaboradorResponse colaborador,
        ColaboradorResponse tecnicoAtribuido,
        AtivoResponse ativo,
        LocalDateTime dataAbertura,
        LocalDateTime dataLimiteSla,
        LocalDateTime dataFechamento,
        boolean slaEstourado,
        int horasSlaPadrao
) {
    public static TicketResponse fromEntity(Ticket t) {
        if (t == null) {
            return null;
        }
        return new TicketResponse(
                t.getId(),
                t.getProtocolo(),
                t.getTitulo(),
                t.getDescricao(),
                t.getCategoria(),
                t.getPrioridade(),
                t.getStatus(),
                ColaboradorResponse.fromEntity(t.getColaborador()),
                ColaboradorResponse.fromEntity(t.getTecnicoAtribuido()),
                AtivoResponse.fromEntity(t.getAtivo()),
                t.getDataAbertura(),
                t.getDataLimiteSla(),
                t.getDataFechamento(),
                t.isSlaEstourado(),
                t.getPrioridade().getHorasSla()
        );
    }
}
