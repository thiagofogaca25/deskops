package com.deskops.domain.entity;

import com.deskops.domain.enums.StatusAtivo;
import com.deskops.domain.enums.TipoAtivo;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "ativos")
public class Ativo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tag_patrimonio", nullable = false, unique = true, length = 30)
    private String tagPatrimonio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAtivo tipo;

    @Column(nullable = false, length = 100)
    private String fabricante;

    @Column(nullable = false, length = 120)
    private String modelo;

    @Column(name = "numero_serie", length = 100)
    private String numeroSerie;

    @Column(length = 120)
    private String processador;

    @Column(name = "memoria_ram", length = 60)
    private String memoriaRam;

    @Column(length = 100)
    private String armazenamento;

    @Column(name = "sistema_operacional", length = 100)
    private String sistemaOperacional;

    @Column(name = "endereco_mac", length = 30)
    private String enderecoMac;

    @Column(name = "ip_local", length = 45)
    private String ipLocal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusAtivo status = StatusAtivo.DISPONIVEL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colaborador_id")
    private Colaborador colaborador;

    public Ativo() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTagPatrimonio() {
        return tagPatrimonio;
    }

    public void setTagPatrimonio(String tagPatrimonio) {
        this.tagPatrimonio = tagPatrimonio;
    }

    public TipoAtivo getTipo() {
        return tipo;
    }

    public void setTipo(TipoAtivo tipo) {
        this.tipo = tipo;
    }

    public String getFabricante() {
        return fabricante;
    }

    public void setFabricante(String fabricante) {
        this.fabricante = fabricante;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public String getProcessador() {
        return processador;
    }

    public void setProcessador(String processador) {
        this.processador = processador;
    }

    public String getMemoriaRam() {
        return memoriaRam;
    }

    public void setMemoriaRam(String memoriaRam) {
        this.memoriaRam = memoriaRam;
    }

    public String getArmazenamento() {
        return armazenamento;
    }

    public void setArmazenamento(String armazenamento) {
        this.armazenamento = armazenamento;
    }

    public String getSistemaOperacional() {
        return sistemaOperacional;
    }

    public void setSistemaOperacional(String sistemaOperacional) {
        this.sistemaOperacional = sistemaOperacional;
    }

    public String getEnderecoMac() {
        return enderecoMac;
    }

    public void setEnderecoMac(String enderecoMac) {
        this.enderecoMac = enderecoMac;
    }

    public String getIpLocal() {
        return ipLocal;
    }

    public void setIpLocal(String ipLocal) {
        this.ipLocal = ipLocal;
    }

    public StatusAtivo getStatus() {
        return status;
    }

    public void setStatus(StatusAtivo status) {
        this.status = status;
    }

    public Colaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(Colaborador colaborador) {
        this.colaborador = colaborador;
    }
}
