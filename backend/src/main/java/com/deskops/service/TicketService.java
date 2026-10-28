package com.deskops.service;

import com.deskops.domain.entity.Ativo;
import com.deskops.domain.entity.Colaborador;
import com.deskops.domain.entity.Ticket;
import com.deskops.domain.entity.TicketNota;
import com.deskops.domain.enums.*;
import com.deskops.dto.*;
import com.deskops.exception.BusinessRuleException;
import com.deskops.exception.ResourceNotFoundException;
import com.deskops.repository.AtivoRepository;
import com.deskops.repository.TicketNotaRepository;
import com.deskops.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TicketService {

    private static final DateTimeFormatter PROTOCOLO_MES_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final TicketRepository ticketRepository;
    private final TicketNotaRepository ticketNotaRepository;
    private final AtivoRepository ativoRepository;
    private final ColaboradorService colaboradorService;
    private final AtivoService ativoService;

    public TicketService(
            TicketRepository ticketRepository,
            TicketNotaRepository ticketNotaRepository,
            AtivoRepository ativoRepository,
            ColaboradorService colaboradorService,
            AtivoService ativoService
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketNotaRepository = ticketNotaRepository;
        this.ativoRepository = ativoRepository;
        this.colaboradorService = colaboradorService;
        this.ativoService = ativoService;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listarComFiltros(
            StatusTicket status,
            PrioridadeTicket prioridade,
            CategoriaTicket categoria,
            UUID ativoId
    ) {
        return ticketRepository.buscarComFiltros(status, prioridade, categoria, ativoId)
                .stream()
                .map(TicketResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse buscarPorId(UUID id) {
        return TicketResponse.fromEntity(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public Ticket buscarEntidadePorId(UUID id) {
        return ticketRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado (Ticket) não encontrado com o ID: " + id));
    }

    @Transactional
    public TicketResponse abrirChamado(TicketRequest request) {
        Colaborador solicitante = colaboradorService.buscarEntidadePorId(request.colaboradorId());

        Ativo ativoVinculado = null;
        if (request.ativoId() != null) {
            ativoVinculado = ativoService.buscarEntidadePorId(request.ativoId());
            if (ativoVinculado.getStatus() == StatusAtivo.DESCARTADO) {
                throw new BusinessRuleException(
                        "Não é permitido abrir chamado para o ativo " + ativoVinculado.getTagPatrimonio() + " pois ele está DESCARTADO."
                );
            }
        }

        Colaborador tecnico = null;
        if (request.tecnicoAtribuidoId() != null) {
            tecnico = colaboradorService.buscarEntidadePorId(request.tecnicoAtribuidoId());
        }

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime limiteSla = calcularDataLimiteSla(agora, request.prioridade());
        String protocolo = gerarProximoProtocolo(agora);

        Ticket ticket = new Ticket();
        ticket.setProtocolo(protocolo);
        ticket.setTitulo(request.titulo().trim());
        ticket.setDescricao(request.descricao().trim());
        ticket.setCategoria(request.categoria());
        ticket.setPrioridade(request.prioridade());
        ticket.setStatus(StatusTicket.ABERTO);
        ticket.setColaborador(solicitante);
        ticket.setTecnicoAtribuido(tecnico);
        ticket.setAtivo(ativoVinculado);
        ticket.setDataAbertura(agora);
        ticket.setDataLimiteSla(limiteSla);

        Ticket salvo = ticketRepository.save(ticket);

        if (ativoVinculado != null) {
            ativoService.registrarHistorico(
                    ativoVinculado,
                    String.format("Chamado [%s] (%s - %s) aberto para este ativo: %s",
                            salvo.getProtocolo(), salvo.getCategoria(), salvo.getPrioridade(), salvo.getTitulo())
            );
        }

        return TicketResponse.fromEntity(salvo);
    }

    public LocalDateTime calcularDataLimiteSla(LocalDateTime dataAbertura, PrioridadeTicket prioridade) {
        return switch (prioridade) {
            case CRITICA -> dataAbertura.plusHours(2);
            case ALTA -> dataAbertura.plusHours(8);
            case MEDIA -> dataAbertura.plusHours(24);
            case BAIXA -> dataAbertura.plusHours(48);
        };
    }

    public synchronized String gerarProximoProtocolo(LocalDateTime referencia) {
        String anoMes = YearMonth.from(referencia).format(PROTOCOLO_MES_FORMATTER);
        String prefixo = "TKT-" + anoMes + "-";
        long existentes = ticketRepository.countByProtocoloStartingWith(prefixo);
        long sequencial = existentes + 1;
        String candidato = prefixo + String.format("%04d", sequencial);
        while (ticketRepository.existsByProtocolo(candidato)) {
            sequencial++;
            candidato = prefixo + String.format("%04d", sequencial);
        }
        return candidato;
    }

    @Transactional
    public TicketResponse atualizarStatus(UUID id, TicketStatusUpdateRequest request) {
        Ticket ticket = buscarEntidadePorId(id);
        StatusTicket statusAtual = ticket.getStatus();
        StatusTicket novoStatus = request.status();

        if (statusAtual == StatusTicket.FECHADO) {
            throw new BusinessRuleException("Chamados com status FECHADO são imutáveis e não permitem novas transições.");
        }

        validarTransicaoEstado(statusAtual, novoStatus);

        if (request.tecnicoAtribuidoId() != null) {
            Colaborador tecnico = colaboradorService.buscarEntidadePorId(request.tecnicoAtribuidoId());
            ticket.setTecnicoAtribuido(tecnico);
        }

        if (novoStatus == StatusTicket.EM_ATENDIMENTO && ticket.getTecnicoAtribuido() == null) {
            throw new BusinessRuleException("Para transicionar o chamado para EM_ATENDIMENTO é obrigatório atribuir um técnico responsável.");
        }

        if (novoStatus == StatusTicket.RESOLVIDO || novoStatus == StatusTicket.FECHADO) {
            if (ticket.getDataFechamento() == null) {
                ticket.setDataFechamento(LocalDateTime.now());
            }
        } else if (statusAtual == StatusTicket.RESOLVIDO && novoStatus == StatusTicket.EM_ATENDIMENTO) {
            ticket.setDataFechamento(null);
        }

        ticket.setStatus(novoStatus);
        Ticket salvo = ticketRepository.save(ticket);

        sincronizarAtivoNaTransicao(salvo, statusAtual, novoStatus);

        if (request.observacaoTransicao() != null && !request.observacaoTransicao().isBlank()) {
            Colaborador autorNota = salvo.getTecnicoAtribuido() != null
                    ? salvo.getTecnicoAtribuido()
                    : salvo.getColaborador();
            boolean privada = Boolean.TRUE.equals(request.notaPrivadaTi());
            TicketNota nota = new TicketNota(
                    salvo,
                    autorNota,
                    String.format("[%s -> %s] %s", statusAtual, novoStatus, request.observacaoTransicao().trim()),
                    privada,
                    LocalDateTime.now()
            );
            ticketNotaRepository.save(nota);
        }

        return TicketResponse.fromEntity(salvo);
    }

    public void validarTransicaoEstado(StatusTicket atual, StatusTicket novo) {
        if (atual == novo) {
            return;
        }
        Set<StatusTicket> permitidos = switch (atual) {
            case ABERTO -> EnumSet.of(StatusTicket.EM_ATENDIMENTO, StatusTicket.FECHADO);
            case EM_ATENDIMENTO -> EnumSet.of(
                    StatusTicket.AGUARDANDO_USUARIO,
                    StatusTicket.AGUARDANDO_PECA,
                    StatusTicket.RESOLVIDO
            );
            case AGUARDANDO_USUARIO -> EnumSet.of(
                    StatusTicket.EM_ATENDIMENTO,
                    StatusTicket.RESOLVIDO,
                    StatusTicket.FECHADO
            );
            case AGUARDANDO_PECA -> EnumSet.of(
                    StatusTicket.EM_ATENDIMENTO,
                    StatusTicket.RESOLVIDO
            );
            case RESOLVIDO -> EnumSet.of(
                    StatusTicket.FECHADO,
                    StatusTicket.EM_ATENDIMENTO
            );
            case FECHADO -> EnumSet.noneOf(StatusTicket.class);
        };

        if (!permitidos.contains(novo)) {
            throw new BusinessRuleException(
                    String.format("Transição de estado inválida de %s para %s. Transições permitidas: %s",
                            atual, novo, permitidos)
            );
        }
    }

    private void sincronizarAtivoNaTransicao(Ticket ticket, StatusTicket statusAnterior, StatusTicket novoStatus) {
        Ativo ativo = ticket.getAtivo();
        if (ativo == null || ticket.getCategoria() != CategoriaTicket.HARDWARE) {
            return;
        }

        if (novoStatus == StatusTicket.AGUARDANDO_PECA || novoStatus == StatusTicket.EM_ATENDIMENTO) {
            if (novoStatus == StatusTicket.AGUARDANDO_PECA && ativo.getStatus() != StatusAtivo.EM_MANUTENCAO) {
                ativo.setStatus(StatusAtivo.EM_MANUTENCAO);
                ativoRepository.save(ativo);
            }
            ativoService.registrarHistorico(
                    ativo,
                    String.format("Chamado de hardware [%s] avançou de %s para %s (Status do ativo: %s).",
                            ticket.getProtocolo(), statusAnterior, novoStatus, ativo.getStatus())
            );
        } else if (novoStatus == StatusTicket.RESOLVIDO || novoStatus == StatusTicket.FECHADO) {
            if (ativo.getStatus() == StatusAtivo.EM_MANUTENCAO) {
                StatusAtivo restaurado = ativo.getColaborador() != null ? StatusAtivo.EM_USO : StatusAtivo.DISPONIVEL;
                ativo.setStatus(restaurado);
                ativoRepository.save(ativo);
            }
            ativoService.registrarHistorico(
                    ativo,
                    String.format("Manutenção concluída no chamado [%s] (%s). Status operacional do ativo: %s.",
                            ticket.getProtocolo(), novoStatus, ativo.getStatus())
            );
        }
    }

    @Transactional
    public TicketNotaResponse adicionarNota(UUID ticketId, TicketNotaRequest request) {
        Ticket ticket = buscarEntidadePorId(ticketId);
        if (ticket.getStatus() == StatusTicket.FECHADO) {
            throw new BusinessRuleException("Não é possível adicionar notas a um chamado que já está FECHADO.");
        }

        Colaborador autor = colaboradorService.buscarEntidadePorId(request.autorId());
        TicketNota nota = new TicketNota(
                ticket,
                autor,
                request.conteudo().trim(),
                request.isPrivadaTi(),
                LocalDateTime.now()
        );

        return TicketNotaResponse.fromEntity(ticketNotaRepository.save(nota));
    }

    @Transactional(readOnly = true)
    public List<TicketNotaResponse> listarNotas(UUID ticketId, boolean incluirPrivadas) {
        buscarEntidadePorId(ticketId);
        return ticketNotaRepository.findByTicketIdWithAutor(ticketId, incluirPrivadas)
                .stream()
                .map(TicketNotaResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardMetricasResponse calcularMetricasDashboard() {
        List<Ticket> todosTickets = ticketRepository.buscarComFiltros(null, null, null, null);
        List<Ativo> todosAtivos = ativoRepository.findAll();

        long totalTickets = todosTickets.size();
        long abertos = todosTickets.stream().filter(t -> t.getStatus() == StatusTicket.ABERTO).count();
        long emAtendimento = todosTickets.stream().filter(t -> t.getStatus() == StatusTicket.EM_ATENDIMENTO).count();
        long aguardando = todosTickets.stream()
                .filter(t -> t.getStatus() == StatusTicket.AGUARDANDO_USUARIO || t.getStatus() == StatusTicket.AGUARDANDO_PECA)
                .count();
        long concluidos = todosTickets.stream()
                .filter(t -> t.getStatus() == StatusTicket.RESOLVIDO || t.getStatus() == StatusTicket.FECHADO)
                .count();
        long criticosAtivos = todosTickets.stream()
                .filter(t -> t.getPrioridade() == PrioridadeTicket.CRITICA
                        && t.getStatus() != StatusTicket.RESOLVIDO
                        && t.getStatus() != StatusTicket.FECHADO)
                .count();
        long slaViolado = todosTickets.stream().filter(Ticket::isSlaEstourado).count();

        double taxaCumprimentoSla = totalTickets == 0
                ? 100.0
                : Math.round(((double) (totalTickets - slaViolado) / totalTickets) * 1000.0) / 10.0;

        List<Ticket> comFechamento = todosTickets.stream()
                .filter(t -> t.getDataFechamento() != null && t.getDataAbertura() != null)
                .toList();

        double mttrHoras = 0.0;
        if (!comFechamento.isEmpty()) {
            double somaMinutos = comFechamento.stream()
                    .mapToDouble(t -> Duration.between(t.getDataAbertura(), t.getDataFechamento()).toMinutes())
                    .sum();
            mttrHoras = Math.round(((somaMinutos / comFechamento.size()) / 60.0) * 10.0) / 10.0;
        }

        long totalAtivos = todosAtivos.size();
        long ativosEmUso = todosAtivos.stream().filter(a -> a.getStatus() == StatusAtivo.EM_USO).count();
        long ativosDisponiveis = todosAtivos.stream().filter(a -> a.getStatus() == StatusAtivo.DISPONIVEL).count();
        long ativosEmManutencao = todosAtivos.stream().filter(a -> a.getStatus() == StatusAtivo.EM_MANUTENCAO).count();

        Map<String, Long> porStatus = new LinkedHashMap<>();
        for (StatusTicket st : StatusTicket.values()) {
            porStatus.put(st.name(), todosTickets.stream().filter(t -> t.getStatus() == st).count());
        }

        Map<String, Long> porPrioridade = new LinkedHashMap<>();
        for (PrioridadeTicket pr : PrioridadeTicket.values()) {
            porPrioridade.put(pr.name(), todosTickets.stream().filter(t -> t.getPrioridade() == pr).count());
        }

        Map<String, Long> porCategoria = new LinkedHashMap<>();
        for (CategoriaTicket cat : CategoriaTicket.values()) {
            porCategoria.put(cat.name(), todosTickets.stream().filter(t -> t.getCategoria() == cat).count());
        }

        return new DashboardMetricasResponse(
                totalTickets,
                abertos,
                emAtendimento,
                aguardando,
                concluidos,
                criticosAtivos,
                slaViolado,
                taxaCumprimentoSla,
                mttrHoras,
                totalAtivos,
                ativosEmUso,
                ativosDisponiveis,
                ativosEmManutencao,
                porStatus,
                porPrioridade,
                porCategoria
        );
    }
}
