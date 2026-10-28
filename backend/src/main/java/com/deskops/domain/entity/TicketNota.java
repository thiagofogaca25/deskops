package com.deskops.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_notas")
public class TicketNota {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Colaborador autor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "is_privada_ti", nullable = false)
    private boolean privadaTi;

    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro;

    public TicketNota() {
    }

    public TicketNota(Ticket ticket, Colaborador autor, String conteudo, boolean privadaTi, LocalDateTime dataRegistro) {
        this.ticket = ticket;
        this.autor = autor;
        this.conteudo = conteudo;
        this.privadaTi = privadaTi;
        this.dataRegistro = dataRegistro != null ? dataRegistro : LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Colaborador getAutor() {
        return autor;
    }

    public void setAutor(Colaborador autor) {
        this.autor = autor;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public boolean isPrivadaTi() {
        return privadaTi;
    }

    public void setPrivadaTi(boolean privadaTi) {
        this.privadaTi = privadaTi;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDateTime dataRegistro) {
        this.dataRegistro = dataRegistro;
    }
}
