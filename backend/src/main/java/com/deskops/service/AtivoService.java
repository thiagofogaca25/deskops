package com.deskops.service;

import com.deskops.domain.entity.Ativo;
import com.deskops.domain.entity.Colaborador;
import com.deskops.domain.entity.HistoricoAtivo;
import com.deskops.domain.entity.Ticket;
import com.deskops.domain.enums.CategoriaTicket;
import com.deskops.domain.enums.StatusAtivo;
import com.deskops.domain.enums.TipoAtivo;
import com.deskops.dto.*;
import com.deskops.exception.BusinessConflictException;
import com.deskops.exception.BusinessRuleException;
import com.deskops.exception.ResourceNotFoundException;
import com.deskops.repository.AtivoRepository;
import com.deskops.repository.HistoricoAtivoRepository;
import com.deskops.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AtivoService {

    private final AtivoRepository ativoRepository;
    private final HistoricoAtivoRepository historicoAtivoRepository;
    private final TicketRepository ticketRepository;
    private final ColaboradorService colaboradorService;

    public AtivoService(
            AtivoRepository ativoRepository,
            HistoricoAtivoRepository historicoAtivoRepository,
            TicketRepository ticketRepository,
            ColaboradorService colaboradorService
    ) {
        this.ativoRepository = ativoRepository;
        this.historicoAtivoRepository = historicoAtivoRepository;
        this.ticketRepository = ticketRepository;
        this.colaboradorService = colaboradorService;
    }

    @Transactional(readOnly = true)
    public List<AtivoResponse> listarComFiltros(StatusAtivo status, TipoAtivo tipo, String busca) {
        return ativoRepository.buscarComFiltros(status, tipo, busca)
                .stream()
                .map(AtivoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AtivoResponse buscarPorId(UUID id) {
        return AtivoResponse.fromEntity(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public Ativo buscarEntidadePorId(UUID id) {
        return ativoRepository.findByIdWithColaborador(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ativo não encontrado com o ID: " + id));
    }

    @Transactional
    public AtivoResponse criar(AtivoRequest request) {
        String tag = request.tagPatrimonio().trim().toUpperCase();
        if (ativoRepository.existsByTagPatrimonioIgnoreCase(tag)) {
            throw new BusinessConflictException("Já existe um ativo cadastrado com a tag de patrimônio: " + tag);
        }

        StatusAtivo statusDefinido = request.status() != null
                ? request.status()
                : (request.colaboradorId() != null ? StatusAtivo.EM_USO : StatusAtivo.DISPONIVEL);

        if (statusDefinido == StatusAtivo.DESCARTADO && request.colaboradorId() != null) {
            throw new BusinessRuleException("Um ativo DESCARTADO não pode ser associado a um colaborador.");
        }

        Colaborador responsavel = null;
        if (request.colaboradorId() != null) {
            responsavel = colaboradorService.buscarEntidadePorId(request.colaboradorId());
        }

        Ativo ativo = new Ativo();
        ativo.setTagPatrimonio(tag);
        ativo.setTipo(request.tipo());
        ativo.setFabricante(request.fabricante().trim());
        ativo.setModelo(request.modelo().trim());
        ativo.setNumeroSerie(request.numeroSerie());
        ativo.setProcessador(request.processador());
        ativo.setMemoriaRam(request.memoriaRam());
        ativo.setArmazenamento(request.armazenamento());
        ativo.setSistemaOperacional(request.sistemaOperacional());
        ativo.setEnderecoMac(request.enderecoMac());
        ativo.setIpLocal(request.ipLocal());
        ativo.setStatus(statusDefinido);
        ativo.setColaborador(responsavel);

        Ativo salvo = ativoRepository.save(ativo);

        String obsInicial = responsavel != null
                ? String.format("Ativo cadastrado no inventário (%s) e entregue ao colaborador %s (%s).",
                salvo.getStatus(), responsavel.getNome(), responsavel.getDepartamento())
                : String.format("Ativo cadastrado no inventário com status inicial %s.", salvo.getStatus());

        registrarHistorico(salvo, obsInicial);

        return AtivoResponse.fromEntity(salvo);
    }

    @Transactional
    public AtivoResponse atualizarStatusOuPosse(UUID id, AtivoStatusUpdateRequest request) {
        Ativo ativo = buscarEntidadePorId(id);

        if (request.status() == StatusAtivo.DESCARTADO && request.colaboradorId() != null) {
            throw new BusinessRuleException("Um ativo DESCARTADO não pode ser associado a um colaborador.");
        }

        StatusAtivo statusAnterior = ativo.getStatus();
        Colaborador colaboradorAnterior = ativo.getColaborador();

        Colaborador novoColaborador = null;
        if (request.colaboradorId() != null) {
            novoColaborador = colaboradorService.buscarEntidadePorId(request.colaboradorId());
        }

        ativo.setStatus(request.status());
        ativo.setColaborador(novoColaborador);

        Ativo atualizado = ativoRepository.save(ativo);

        StringBuilder resumo = new StringBuilder();
        if (statusAnterior != request.status()) {
            resumo.append(String.format("Status alterado de %s para %s. ", statusAnterior, request.status()));
        }
        UUID idAnt = colaboradorAnterior != null ? colaboradorAnterior.getId() : null;
        UUID idNovo = novoColaborador != null ? novoColaborador.getId() : null;
        if (!Objects.equals(idAnt, idNovo)) {
            String nomeAnt = colaboradorAnterior != null ? colaboradorAnterior.getNome() : "Estoque TI";
            String nomeNovo = novoColaborador != null ? novoColaborador.getNome() : "Estoque TI";
            resumo.append(String.format("Posse transferida de [%s] para [%s]. ", nomeAnt, nomeNovo));
        }
        if (request.observacao() != null && !request.observacao().isBlank()) {
            resumo.append("Nota: ").append(request.observacao().trim());
        }
        if (resumo.isEmpty()) {
            resumo.append("Atualização cadastral de status/posse registrada.");
        }

        registrarHistorico(atualizado, resumo.toString().trim());
        return AtivoResponse.fromEntity(atualizado);
    }

    @Transactional
    public void registrarHistorico(Ativo ativo, String observacao) {
        HistoricoAtivo historico = new HistoricoAtivo(ativo, observacao, LocalDateTime.now());
        historicoAtivoRepository.save(historico);
    }

    @Transactional(readOnly = true)
    public List<HistoricoAtivoResponse> listarHistorico(UUID ativoId) {
        buscarEntidadePorId(ativoId);
        return historicoAtivoRepository.findByAtivoIdOrderByDataRegistroDesc(ativoId)
                .stream()
                .map(HistoricoAtivoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AtivoDiagnosticoResponse obterDiagnostico(UUID ativoId) {
        Ativo ativo = buscarEntidadePorId(ativoId);
        List<Ticket> chamados = ticketRepository.findByAtivoIdWithAssociations(ativoId);
        int incidentesHardware = (int) chamados.stream()
                .filter(t -> t.getCategoria() == CategoriaTicket.HARDWARE)
                .count();

        List<TicketResponse> chamadosDto = chamados.stream()
                .map(TicketResponse::fromEntity)
                .toList();

        List<HistoricoAtivoResponse> historicoDto = historicoAtivoRepository
                .findByAtivoIdOrderByDataRegistroDesc(ativoId)
                .stream()
                .map(HistoricoAtivoResponse::fromEntity)
                .toList();

        return new AtivoDiagnosticoResponse(
                AtivoResponse.fromEntity(ativo),
                incidentesHardware,
                chamadosDto,
                historicoDto
        );
    }
}
