package com.deskops.domain.enums;

public enum PrioridadeTicket {
    CRITICA(2),
    ALTA(8),
    MEDIA(24),
    BAIXA(48);

    private final int horasSla;

    PrioridadeTicket(int horasSla) {
        this.horasSla = horasSla;
    }

    public int getHorasSla() {
        return horasSla;
    }
}
