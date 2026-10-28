package com.deskops.service;

import com.deskops.domain.enums.PrioridadeTicket;
import com.deskops.domain.enums.StatusTicket;
import com.deskops.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketServiceTest {

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(null, null, null, null, null);
    }

    @Test
    @DisplayName("Deve calcular data limite de SLA corretamente para cada nível de prioridade")
    void deveCalcularSlaPorPrioridade() {
        LocalDateTime abertura = LocalDateTime.of(2026, 10, 27, 10, 0);

        assertThat(ticketService.calcularDataLimiteSla(abertura, PrioridadeTicket.CRITICA))
                .isEqualTo(abertura.plusHours(2));
        assertThat(ticketService.calcularDataLimiteSla(abertura, PrioridadeTicket.ALTA))
                .isEqualTo(abertura.plusHours(8));
        assertThat(ticketService.calcularDataLimiteSla(abertura, PrioridadeTicket.MEDIA))
                .isEqualTo(abertura.plusHours(24));
        assertThat(ticketService.calcularDataLimiteSla(abertura, PrioridadeTicket.BAIXA))
                .isEqualTo(abertura.plusHours(48));
    }

    @Test
    @DisplayName("Deve permitir transições válidas na máquina de estados do chamado")
    void devePermitirTransicoesValidas() {
        ticketService.validarTransicaoEstado(StatusTicket.ABERTO, StatusTicket.EM_ATENDIMENTO);
        ticketService.validarTransicaoEstado(StatusTicket.EM_ATENDIMENTO, StatusTicket.AGUARDANDO_USUARIO);
        ticketService.validarTransicaoEstado(StatusTicket.EM_ATENDIMENTO, StatusTicket.AGUARDANDO_PECA);
        ticketService.validarTransicaoEstado(StatusTicket.AGUARDANDO_PECA, StatusTicket.RESOLVIDO);
        ticketService.validarTransicaoEstado(StatusTicket.RESOLVIDO, StatusTicket.FECHADO);
        ticketService.validarTransicaoEstado(StatusTicket.RESOLVIDO, StatusTicket.EM_ATENDIMENTO);
    }

    @Test
    @DisplayName("Deve bloquear transições inválidas na máquina de estados do chamado")
    void deveBloquearTransicoesInvalidas() {
        assertThatThrownBy(() ->
                ticketService.validarTransicaoEstado(StatusTicket.ABERTO, StatusTicket.RESOLVIDO)
        )
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Transição de estado inválida de ABERTO para RESOLVIDO");

        assertThatThrownBy(() ->
                ticketService.validarTransicaoEstado(StatusTicket.AGUARDANDO_PECA, StatusTicket.FECHADO)
        )
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Transição de estado inválida de AGUARDANDO_PECA para FECHADO");
    }
}
