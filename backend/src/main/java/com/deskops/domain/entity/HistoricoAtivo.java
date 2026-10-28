package com.deskops.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "historico_ativos")
public class HistoricoAtivo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ativo_id", nullable = false)
    private Ativo ativo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro;

    public HistoricoAtivo() {
    }

    public HistoricoAtivo(Ativo ativo, String observacao, LocalDateTime dataRegistro) {
        this.ativo = ativo;
        this.observacao = observacao;
        this.dataRegistro = dataRegistro != null ? dataRegistro : LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Ativo getAtivo() {
        return ativo;
    }

    public void setAtivo(Ativo ativo) {
        this.ativo = ativo;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDateTime dataRegistro) {
        this.dataRegistro = dataRegistro;
    }
}
