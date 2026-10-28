package com.deskops.domain.entity;

import com.deskops.domain.enums.CategoriaTicket;
import com.deskops.domain.enums.PrioridadeTicket;
import com.deskops.domain.enums.StatusTicket;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String protocolo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaTicket categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PrioridadeTicket prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusTicket status = StatusTicket.ABERTO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Colaborador colaborador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_atribuido_id")
    private Colaborador tecnicoAtribuido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ativo_id")
    private Ativo ativo;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_limite_sla", nullable = false)
    private LocalDateTime dataLimiteSla;

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    public Ticket() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public void setProtocolo(String protocolo) {
        this.protocolo = protocolo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public CategoriaTicket getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaTicket categoria) {
        this.categoria = categoria;
    }

    public PrioridadeTicket getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(PrioridadeTicket prioridade) {
        this.prioridade = prioridade;
    }

    public StatusTicket getStatus() {
        return status;
    }

    public void setStatus(StatusTicket status) {
        this.status = status;
    }

    public Colaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(Colaborador colaborador) {
        this.colaborador = colaborador;
    }

    public Colaborador getTecnicoAtribuido() {
        return tecnicoAtribuido;
    }

    public void setTecnicoAtribuido(Colaborador tecnicoAtribuido) {
        this.tecnicoAtribuido = tecnicoAtribuido;
    }

    public Ativo getAtivo() {
        return ativo;
    }

    public void setAtivo(Ativo ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDateTime dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public LocalDateTime getDataLimiteSla() {
        return dataLimiteSla;
    }

    public void setDataLimiteSla(LocalDateTime dataLimiteSla) {
        this.dataLimiteSla = dataLimiteSla;
    }

    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(LocalDateTime dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    public boolean isSlaEstourado() {
        if (dataLimiteSla == null) {
            return false;
        }
        LocalDateTime referencia = dataFechamento != null ? dataFechamento : LocalDateTime.now();
        return referencia.isAfter(dataLimiteSla);
    }
}
