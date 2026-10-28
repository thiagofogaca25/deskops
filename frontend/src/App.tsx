import React, { useEffect, useMemo, useState } from 'react';
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Cpu,
  HardDrive,
  Kanban,
  Laptop,
  Lock,
  MessageSquare,
  Monitor,
  Network,
  Plus,
  RefreshCw,
  Search,
  Server,
  ShieldAlert,
  Tag,
  User,
  Wrench,
  X,
} from 'lucide-react';
import { deskopsApi } from './services/api';
import type {
  Ativo,
  AtivoDiagnostico,
  CategoriaTicket,
  Colaborador,
  DashboardMetricas,
  PrioridadeTicket,
  StatusAtivo,
  StatusTicket,
  Ticket,
  TicketNota,
  TipoAtivo,
} from './types/deskops';

type ActiveTab = 'kanban' | 'inventario' | 'diagnostico';

const KANBAN_COLUMNS: { key: StatusTicket; label: string; subtitle: string }[] = [
  { key: 'ABERTO', label: 'Aberto', subtitle: 'Fila de triagem N1' },
  { key: 'EM_ATENDIMENTO', label: 'Em Atendimento', subtitle: 'Atribuído ao técnico' },
  { key: 'AGUARDANDO_USUARIO', label: 'Aguard. Usuário', subtitle: 'Validação / testes' },
  { key: 'AGUARDANDO_PECA', label: 'Aguard. Peça', subtitle: 'Reposição de hardware' },
  { key: 'RESOLVIDO', label: 'Resolvido', subtitle: 'Solução documentada' },
  { key: 'FECHADO', label: 'Fechado', subtitle: 'Concluído e auditado' },
];

const TRANSICOES_VALIDAS: Record<StatusTicket, StatusTicket[]> = {
  ABERTO: ['EM_ATENDIMENTO', 'FECHADO'],
  EM_ATENDIMENTO: ['AGUARDANDO_USUARIO', 'AGUARDANDO_PECA', 'RESOLVIDO'],
  AGUARDANDO_USUARIO: ['EM_ATENDIMENTO', 'RESOLVIDO', 'FECHADO'],
  AGUARDANDO_PECA: ['EM_ATENDIMENTO', 'RESOLVIDO'],
  RESOLVIDO: ['FECHADO', 'EM_ATENDIMENTO'],
  FECHADO: [],
};

export function getPrioridadeBadgeClasses(prioridade: PrioridadeTicket): string {
  switch (prioridade) {
    case 'CRITICA':
      return 'bg-red-500/20 text-red-300 border-red-500/40';
    case 'ALTA':
      return 'bg-orange-500/20 text-orange-300 border-orange-500/40';
    case 'MEDIA':
      return 'bg-amber-500/20 text-amber-300 border-amber-500/40';
    case 'BAIXA':
      return 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40';
  }
}

export function getStatusAtivoBadgeClasses(status: StatusAtivo): string {
  switch (status) {
    case 'DISPONIVEL':
      return 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40';
    case 'EM_USO':
      return 'bg-blue-500/20 text-blue-300 border-blue-500/40';
    case 'EM_MANUTENCAO':
      return 'bg-amber-500/20 text-amber-300 border-amber-500/40';
    case 'DESCARTADO':
      return 'bg-rose-500/20 text-rose-300 border-rose-500/40';
  }
}

function formatDateTime(iso?: string | null): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function App() {
  const [activeTab, setActiveTab] = useState<ActiveTab>('kanban');
  const [loading, setLoading] = useState<boolean>(true);
  const [errorBanner, setErrorBanner] = useState<string | null>(null);

  const [metricas, setMetricas] = useState<DashboardMetricas | null>(null);
  const [healthStatus, setHealthStatus] = useState<string>('UP');
  const [colaboradores, setColaboradores] = useState<Colaborador[]>([]);
  const [ativos, setAtivos] = useState<Ativo[]>([]);
  const [tickets, setTickets] = useState<Ticket[]>([]);

  // Filtros do Kanban
  const [filtroPrioridade, setFiltroPrioridade] = useState<string>('');
  const [filtroCategoria, setFiltroCategoria] = useState<string>('');
  const [buscaKanban, setBuscaKanban] = useState<string>('');

  // Filtros do Inventário
  const [buscaAtivo, setBuscaAtivo] = useState<string>('');
  const [filtroStatusAtivo, setFiltroStatusAtivo] = useState<string>('');
  const [filtroTipoAtivo, setFiltroTipoAtivo] = useState<string>('');

  // Diagnóstico do Ativo selecionado
  const [ativoDiagnostico, setAtivoDiagnostico] = useState<AtivoDiagnostico | null>(null);
  const [loadingDiagnostico, setLoadingDiagnostico] = useState<boolean>(false);

  // Modal de Detalhes / Timeline do Chamado
  const [selectedTicket, setSelectedTicket] = useState<Ticket | null>(null);
  const [ticketNotas, setTicketNotas] = useState<TicketNota[]>([]);
  const [incluirNotasPrivadas, setIncluirNotasPrivadas] = useState<boolean>(true);
  const [novaNotaConteudo, setNovaNotaConteudo] = useState<string>('');
  const [novaNotaPrivada, setNovaNotaPrivada] = useState<boolean>(false);
  const [novaNotaAutorId, setNovaNotaAutorId] = useState<string>('');
  const [tecnicoTransicaoId, setTecnicoTransicaoId] = useState<string>('');
  const [obsTransicao, setObsTransicao] = useState<string>('');

  // Modal de Novo Chamado
  const [showNovoTicketModal, setShowNovoTicketModal] = useState<boolean>(false);
  const [novoTicketForm, setNovoTicketForm] = useState({
    titulo: '',
    descricao: '',
    categoria: 'HARDWARE' as CategoriaTicket,
    prioridade: 'ALTA' as PrioridadeTicket,
    colaboradorId: '',
    ativoId: '',
    tecnicoAtribuidoId: '',
  });

  // Modal de Novo Ativo
  const [showNovoAtivoModal, setShowNovoAtivoModal] = useState<boolean>(false);
  const [novoAtivoForm, setNovoAtivoForm] = useState({
    tagPatrimonio: 'TI-00310',
    tipo: 'NOTEBOOK' as TipoAtivo,
    fabricante: 'Dell',
    modelo: 'Latitude 5440',
    numeroSerie: 'BR-DL5440-3100',
    processador: 'Intel Core i7-1355U',
    memoriaRam: '16 GB DDR4',
    armazenamento: '512 GB NVMe SSD',
    sistemaOperacional: 'Windows 11 Pro 23H2',
    enderecoMac: '00:1B:44:99:1A:2B',
    ipLocal: '192.168.10.145',
    status: 'EM_USO' as StatusAtivo,
    colaboradorId: '',
  });

  const carregarDados = async () => {
    setLoading(true);
    setErrorBanner(null);
    try {
      const [met, hlth, colabs, atvs, tkts] = await Promise.all([
        deskopsApi.getMetricas(),
        deskopsApi.getHealth(),
        deskopsApi.getColaboradores(),
        deskopsApi.getAtivos(),
        deskopsApi.getTickets(),
      ]);
      setMetricas(met);
      setHealthStatus(hlth.status || 'UP');
      setColaboradores(colabs);
      setAtivos(atvs);
      setTickets(tkts);

      if (colabs.length > 0 && !novoTicketForm.colaboradorId) {
        setNovoTicketForm((prev) => ({ ...prev, colaboradorId: colabs[0].id }));
        setNovaNotaAutorId(colabs[0].id);
      }
      if (atvs.length > 0 && !ativoDiagnostico) {
        const diag = await deskopsApi.getAtivoDiagnostico(atvs[0].id);
        setAtivoDiagnostico(diag);
      }
    } catch (err) {
      setErrorBanner(
        err instanceof Error ? err.message : 'Falha ao carregar dados da API DeskOps.'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void carregarDados();
  }, []);

  const abrirDiagnosticoAtivo = async (ativoId: string) => {
    setActiveTab('diagnostico');
    setLoadingDiagnostico(true);
    setErrorBanner(null);
    try {
      const diag = await deskopsApi.getAtivoDiagnostico(ativoId);
      setAtivoDiagnostico(diag);
    } catch (err) {
      setErrorBanner(
        err instanceof Error ? err.message : 'Erro ao consultar diagnóstico do ativo.'
      );
    } finally {
      setLoadingDiagnostico(false);
    }
  };

  const abrirDetalhesTicket = async (ticket: Ticket) => {
    setSelectedTicket(ticket);
    setTecnicoTransicaoId(ticket.tecnicoAtribuido?.id || '');
    setObsTransicao('');
    try {
      const notas = await deskopsApi.getTicketNotas(ticket.id, incluirNotasPrivadas);
      setTicketNotas(notas);
    } catch {
      setTicketNotas([]);
    }
  };

  const alternarVisibilidadeNotas = async (incluirPrivadas: boolean) => {
    setIncluirNotasPrivadas(incluirPrivadas);
    if (!selectedTicket) return;
    try {
      const notas = await deskopsApi.getTicketNotas(selectedTicket.id, incluirPrivadas);
      setTicketNotas(notas);
    } catch {
      // ignore
    }
  };

  const handleTransicaoTicket = async (novoStatus: StatusTicket) => {
    if (!selectedTicket) return;
    setErrorBanner(null);
    try {
      const atualizado = await deskopsApi.atualizarStatusTicket(selectedTicket.id, {
        status: novoStatus,
        tecnicoAtribuidoId: tecnicoTransicaoId || null,
        observacaoTransicao: obsTransicao || undefined,
        notaPrivadaTi: false,
      });
      setSelectedTicket(atualizado);
      setObsTransicao('');
      const notas = await deskopsApi.getTicketNotas(atualizado.id, incluirNotasPrivadas);
      setTicketNotas(notas);
      await carregarDados();
    } catch (err) {
      setErrorBanner(
        err instanceof Error ? err.message : 'Erro ao transicionar status do chamado.'
      );
    }
  };

  const handleAdicionarNota = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTicket || !novaNotaConteudo.trim() || !novaNotaAutorId) return;
    setErrorBanner(null);
    try {
      await deskopsApi.adicionarTicketNota(selectedTicket.id, {
        autorId: novaNotaAutorId,
        conteudo: novaNotaConteudo.trim(),
        isPrivadaTi: novaNotaPrivada,
      });
      setNovaNotaConteudo('');
      const notas = await deskopsApi.getTicketNotas(
        selectedTicket.id,
        incluirNotasPrivadas
      );
      setTicketNotas(notas);
    } catch (err) {
      setErrorBanner(err instanceof Error ? err.message : 'Erro ao registrar nota.');
    }
  };

  const handleCriarTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorBanner(null);
    try {
      await deskopsApi.abrirTicket({
        titulo: novoTicketForm.titulo,
        descricao: novoTicketForm.descricao,
        categoria: novoTicketForm.categoria,
        prioridade: novoTicketForm.prioridade,
        colaboradorId: novoTicketForm.colaboradorId,
        ativoId: novoTicketForm.ativoId || null,
        tecnicoAtribuidoId: novoTicketForm.tecnicoAtribuidoId || null,
      });
      setShowNovoTicketModal(false);
      setNovoTicketForm((prev) => ({
        ...prev,
        titulo: '',
        descricao: '',
        ativoId: '',
      }));
      await carregarDados();
    } catch (err) {
      setErrorBanner(err instanceof Error ? err.message : 'Erro ao abrir chamado.');
    }
  };

  const handleCriarAtivo = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorBanner(null);
    try {
      await deskopsApi.criarAtivo({
        ...novoAtivoForm,
        colaboradorId: novoAtivoForm.colaboradorId || null,
      });
      setShowNovoAtivoModal(false);
      await carregarDados();
    } catch (err) {
      setErrorBanner(err instanceof Error ? err.message : 'Erro ao cadastrar ativo.');
    }
  };

  const tecnicosDisponiveis = useMemo(
    () =>
      colaboradores.filter(
        (c) => c.perfil === 'TECNICO' || c.perfil === 'ADMIN'
      ),
    [colaboradores]
  );

  const ticketsFiltrados = useMemo(() => {
    return tickets.filter((t) => {
      if (filtroPrioridade && t.prioridade !== filtroPrioridade) return false;
      if (filtroCategoria && t.categoria !== filtroCategoria) return false;
      if (buscaKanban.trim()) {
        const q = buscaKanban.toLowerCase();
        const matchProtocolo = t.protocolo.toLowerCase().includes(q);
        const matchTitulo = t.titulo.toLowerCase().includes(q);
        const matchAtivo = t.ativo?.tagPatrimonio.toLowerCase().includes(q) ?? false;
        const matchSolicitante = t.colaborador.nome.toLowerCase().includes(q);
        if (!matchProtocolo && !matchTitulo && !matchAtivo && !matchSolicitante) {
          return false;
        }
      }
      return true;
    });
  }, [tickets, filtroPrioridade, filtroCategoria, buscaKanban]);

  const ativosFiltrados = useMemo(() => {
    return ativos.filter((a) => {
      if (filtroStatusAtivo && a.status !== filtroStatusAtivo) return false;
      if (filtroTipoAtivo && a.tipo !== filtroTipoAtivo) return false;
      if (buscaAtivo.trim()) {
        const q = buscaAtivo.toLowerCase();
        const matchTag = a.tagPatrimonio.toLowerCase().includes(q);
        const matchIp = (a.ipLocal || '').toLowerCase().includes(q);
        const matchModelo = `${a.fabricante} ${a.modelo}`.toLowerCase().includes(q);
        const matchColab = (a.colaborador?.nome || '').toLowerCase().includes(q);
        if (!matchTag && !matchIp && !matchModelo && !matchColab) return false;
      }
      return true;
    });
  }, [ativos, buscaAtivo, filtroStatusAtivo, filtroTipoAtivo]);

  return (
    <div className="min-h-screen w-full max-w-[100vw] overflow-x-hidden bg-slate-950 text-slate-100 flex flex-col">
      {/* Header Principal Responsivo (Sem overflow no celular) */}
      <header className="border-b border-slate-800 bg-slate-900/90 backdrop-blur sticky top-0 z-30 w-full">
        <div className="max-w-[1600px] mx-auto px-3 sm:px-6 py-3 sm:py-4 flex flex-col lg:flex-row lg:items-center lg:justify-between gap-3">
          {/* Topo Mobile: Logo + Botões Rápidos */}
          <div className="flex items-center justify-between gap-2 w-full lg:w-auto">
            <div className="flex items-center gap-2.5 min-w-0">
              <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-xl bg-blue-600 flex items-center justify-center shadow-lg shadow-blue-600/30 shrink-0">
                <Server className="w-5 h-5 text-white" />
              </div>
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <h1 className="text-lg sm:text-xl font-bold tracking-tight text-white truncate">
                    DeskOps
                  </h1>
                  <span className="text-[10px] sm:text-xs font-mono px-1.5 py-0.5 rounded bg-blue-500/20 text-blue-300 border border-blue-500/30 shrink-0">
                    ITSM + ITAM
                  </span>
                </div>
                <p className="text-[11px] sm:text-xs text-slate-400 truncate">
                  Help Desk, Controle de SLA & Ativos de TI
                </p>
              </div>
            </div>

            {/* Ações rápidas visíveis no topo mobile */}
            <div className="flex lg:hidden items-center gap-1.5 shrink-0">
              <div
                className="flex items-center gap-1.5 px-2 py-1.5 rounded-lg bg-slate-950 border border-slate-800 text-[11px]"
                title="Status do Spring Boot Actuator (/actuator/health)"
              >
                <span
                  className={`w-2 h-2 rounded-full ${
                    healthStatus === 'UP' ? 'bg-emerald-400 animate-pulse' : 'bg-red-400'
                  }`}
                />
                <span className="font-mono font-semibold text-emerald-400">
                  {healthStatus}
                </span>
              </div>

              <button
                type="button"
                onClick={() => setShowNovoTicketModal(true)}
                className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-medium transition shadow"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Chamado</span>
              </button>

              <button
                type="button"
                onClick={() => void carregarDados()}
                className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition"
                title="Atualizar dados"
              >
                <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              </button>
            </div>
          </div>

          {/* Navegação entre os 3 Módulos Obrigatórios (Grid de 3 colunas no celular, sem scroll horizontal) */}
          <nav className="grid grid-cols-3 sm:flex items-center gap-1 sm:gap-2 bg-slate-950 p-1 rounded-xl border border-slate-800 w-full lg:w-auto">
            <button
              type="button"
              onClick={() => setActiveTab('kanban')}
              aria-label="Kanban de Chamados"
              className={`flex items-center justify-center gap-1.5 px-2 sm:px-4 py-2 rounded-lg text-xs sm:text-sm font-medium transition truncate ${
                activeTab === 'kanban'
                  ? 'bg-blue-600 text-white shadow'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Kanban className="w-3.5 h-3.5 sm:w-4 sm:h-4 shrink-0" />
              <span className="truncate">
                <span className="hidden sm:inline">Kanban de </span>Chamados
              </span>
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('inventario')}
              aria-label="Inventário de Ativos"
              className={`flex items-center justify-center gap-1.5 px-2 sm:px-4 py-2 rounded-lg text-xs sm:text-sm font-medium transition truncate ${
                activeTab === 'inventario'
                  ? 'bg-blue-600 text-white shadow'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Laptop className="w-3.5 h-3.5 sm:w-4 sm:h-4 shrink-0" />
              <span className="truncate">
                Inventário<span className="hidden sm:inline"> de Ativos</span>
              </span>
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('diagnostico')}
              aria-label="Diagnóstico do Ativo"
              className={`flex items-center justify-center gap-1.5 px-2 sm:px-4 py-2 rounded-lg text-xs sm:text-sm font-medium transition truncate ${
                activeTab === 'diagnostico'
                  ? 'bg-blue-600 text-white shadow'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Cpu className="w-3.5 h-3.5 sm:w-4 sm:h-4 shrink-0" />
              <span className="truncate">
                Diagnóstico<span className="hidden sm:inline"> do Ativo</span>
              </span>
            </button>
          </nav>

          {/* Ações Rápidas & Status do Actuator (Desktop) */}
          <div className="hidden lg:flex items-center gap-3 shrink-0">
            <div
              className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-950 border border-slate-800 text-xs"
              title="Status do Spring Boot Actuator (/actuator/health)"
            >
              <span
                className={`w-2 h-2 rounded-full ${
                  healthStatus === 'UP' ? 'bg-emerald-400 animate-pulse' : 'bg-red-400'
                }`}
              />
              <span className="text-slate-400">Actuator:</span>
              <span className="font-mono font-semibold text-emerald-400">
                {healthStatus}
              </span>
            </div>

            <button
              type="button"
              onClick={() => setShowNovoTicketModal(true)}
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium transition shadow"
            >
              <Plus className="w-4 h-4" />
              Abrir Chamado
            </button>

            <button
              type="button"
              onClick={() => void carregarDados()}
              className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition"
              title="Atualizar dados"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>
      </header>

      {/* Banner de Erro RFC 7807 */}
      {errorBanner && (
        <div className="max-w-[1600px] w-full mx-auto px-3 sm:px-6 pt-3 sm:pt-4">
          <div
            role="alert"
            className="flex items-center justify-between gap-3 px-3.5 py-2.5 rounded-xl bg-red-500/15 border border-red-500/40 text-red-200 text-xs sm:text-sm"
          >
            <div className="flex items-center gap-2 min-w-0">
              <AlertTriangle className="w-4 h-4 text-red-400 shrink-0" />
              <span className="break-words">{errorBanner}</span>
            </div>
            <button
              type="button"
              onClick={() => setErrorBanner(null)}
              className="text-red-300 hover:text-white shrink-0"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* KPI Cards de Métricas Operacionais de TI (2x2 no celular, 4 colunas no desktop) */}
      <section className="max-w-[1600px] w-full mx-auto px-3 sm:px-6 pt-4 sm:pt-6 grid grid-cols-2 lg:grid-cols-4 gap-2.5 sm:gap-4">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-3 sm:p-4 flex items-center justify-between gap-2 min-w-0">
          <div className="min-w-0">
            <p className="text-[10px] sm:text-xs text-slate-400 uppercase tracking-wider truncate">
              Chamados Ativos
            </p>
            <p className="text-lg sm:text-2xl font-bold text-white mt-0.5 sm:mt-1 truncate">
              {(metricas?.ticketsAbertos ?? 0) +
                (metricas?.ticketsEmAtendimento ?? 0) +
                (metricas?.ticketsAguardando ?? 0)}
              <span className="text-[11px] sm:text-xs font-normal text-slate-400 ml-1.5">
                / {metricas?.totalTickets ?? tickets.length}
              </span>
            </p>
          </div>
          <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-lg bg-blue-500/15 border border-blue-500/30 flex items-center justify-center text-blue-400 shrink-0">
            <Activity className="w-4 h-4 sm:w-5 sm:h-5" />
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-3 sm:p-4 flex items-center justify-between gap-2 min-w-0">
          <div className="min-w-0">
            <p className="text-[10px] sm:text-xs text-slate-400 uppercase tracking-wider truncate">
              SLA Cumprido
            </p>
            <p className="text-lg sm:text-2xl font-bold text-emerald-400 mt-0.5 sm:mt-1 truncate">
              {metricas?.taxaCumprimentoSlaPercentual ?? 100}%
              <span className="hidden sm:inline text-xs font-normal text-slate-400 ml-2">
                ({metricas?.ticketsCriticosAtivos ?? 0} críticos)
              </span>
            </p>
          </div>
          <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-lg bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shrink-0">
            <CheckCircle2 className="w-4 h-4 sm:w-5 sm:h-5" />
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-3 sm:p-4 flex items-center justify-between gap-2 min-w-0">
          <div className="min-w-0">
            <p className="text-[10px] sm:text-xs text-slate-400 uppercase tracking-wider truncate">
              MTTR Médio
            </p>
            <p className="text-lg sm:text-2xl font-bold text-amber-300 mt-0.5 sm:mt-1 truncate">
              {metricas?.mttrHorasMedia ?? 0}h
              <span className="hidden sm:inline text-xs font-normal text-slate-400 ml-2">
                resolução
              </span>
            </p>
          </div>
          <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-lg bg-amber-500/15 border border-amber-500/30 flex items-center justify-center text-amber-400 shrink-0">
            <Clock className="w-4 h-4 sm:w-5 sm:h-5" />
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-3 sm:p-4 flex items-center justify-between gap-2 min-w-0">
          <div className="min-w-0">
            <p className="text-[10px] sm:text-xs text-slate-400 uppercase tracking-wider truncate">
              Ativos (ITAM)
            </p>
            <p className="text-lg sm:text-2xl font-bold text-white mt-0.5 sm:mt-1 truncate">
              {metricas?.totalAtivos ?? ativos.length}
              <span className="text-[11px] sm:text-xs font-normal text-amber-300 ml-1.5">
                ({metricas?.ativosEmManutencao ?? 0} manut.)
              </span>
            </p>
          </div>
          <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-lg bg-purple-500/15 border border-purple-500/30 flex items-center justify-center text-purple-400 shrink-0">
            <Wrench className="w-4 h-4 sm:w-5 sm:h-5" />
          </div>
        </div>
      </section>

      {/* Conteúdo Principal do Módulo Selecionado */}
      <main className="max-w-[1600px] w-full mx-auto px-3 sm:px-6 py-4 sm:py-6 flex-1 min-w-0">
        {/* MÓDULO 1: KANBAN DE CHAMADOS */}
        {activeTab === 'kanban' && (
          <div className="space-y-4">
            {/* Barra de Filtros do Kanban */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-3 sm:p-4 flex flex-col lg:flex-row lg:items-center lg:justify-between gap-3">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 flex-1">
                <div className="relative sm:col-span-1">
                  <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={buscaKanban}
                    onChange={(e) => setBuscaKanban(e.target.value)}
                    placeholder="Buscar protocolo, título ou ativo..."
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3 py-2 text-xs sm:text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-blue-500"
                  />
                </div>

                <select
                  aria-label="Filtrar por prioridade"
                  value={filtroPrioridade}
                  onChange={(e) => setFiltroPrioridade(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs sm:text-sm text-slate-200"
                >
                  <option value="">Todas as Prioridades</option>
                  <option value="CRITICA">Crítica (SLA 2h)</option>
                  <option value="ALTA">Alta (SLA 8h)</option>
                  <option value="MEDIA">Média (SLA 24h)</option>
                  <option value="BAIXA">Baixa (SLA 48h)</option>
                </select>

                <select
                  aria-label="Filtrar por categoria"
                  value={filtroCategoria}
                  onChange={(e) => setFiltroCategoria(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs sm:text-sm text-slate-200"
                >
                  <option value="">Todas as Categorias</option>
                  <option value="HARDWARE">Hardware</option>
                  <option value="SOFTWARE">Software</option>
                  <option value="REDE_ACESSO">Rede / Acesso</option>
                  <option value="TELEFONIA">Telefonia</option>
                  <option value="DUVIDA_GERAL">Dúvida Geral</option>
                </select>
              </div>

              <div className="flex flex-wrap items-center gap-1.5 text-[10px] sm:text-xs text-slate-400">
                <span className="hidden sm:inline">SLA:</span>
                <span className="px-2 py-0.5 rounded border bg-red-500/20 text-red-300 border-red-500/40 font-semibold">
                  CRÍTICA (2h)
                </span>
                <span className="px-2 py-0.5 rounded border bg-orange-500/20 text-orange-300 border-orange-500/40 font-semibold">
                  ALTA (8h)
                </span>
                <span className="px-2 py-0.5 rounded border bg-amber-500/20 text-amber-300 border-amber-500/40 font-semibold">
                  MÉDIA (24h)
                </span>
                <span className="px-2 py-0.5 rounded border bg-emerald-500/20 text-emerald-300 border-emerald-500/40 font-semibold">
                  BAIXA (48h)
                </span>
              </div>
            </div>

            {/* Colunas do Kanban (Compactas no celular, sem min-h exagerado) */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-3 sm:gap-4">
              {KANBAN_COLUMNS.map((col) => {
                const colTickets = ticketsFiltrados.filter(
                  (t) => t.status === col.key
                );
                return (
                  <div
                    key={col.key}
                    className="bg-slate-900/70 border border-slate-800 rounded-xl flex flex-col md:min-h-[500px] min-w-0"
                  >
                    <div className="p-3 border-b border-slate-800 flex items-center justify-between">
                      <div className="min-w-0">
                        <h2 className="text-sm font-semibold text-white truncate">
                          {col.label}
                        </h2>
                        <p className="text-[11px] text-slate-400 truncate">
                          {col.subtitle}
                        </p>
                      </div>
                      <span className="px-2 py-0.5 text-xs font-mono font-bold rounded-full bg-slate-800 text-slate-200 shrink-0">
                        {colTickets.length}
                      </span>
                    </div>

                    <div className="p-2.5 sm:p-3 space-y-2.5 sm:space-y-3 flex-1">
                      {colTickets.length === 0 ? (
                        <div className="py-6 md:h-28 border border-dashed border-slate-800 rounded-lg flex items-center justify-center text-xs text-slate-500">
                          Sem chamados nesta etapa
                        </div>
                      ) : (
                        colTickets.map((ticket) => (
                          <div
                            key={ticket.id}
                            onClick={() => void abrirDetalhesTicket(ticket)}
                            className="bg-slate-950 border border-slate-800 hover:border-blue-500/60 rounded-xl p-3 sm:p-3.5 cursor-pointer transition shadow-sm hover:shadow-md space-y-2"
                          >
                            <div className="flex items-center justify-between gap-2">
                              <span className="font-mono text-xs text-blue-400 font-semibold truncate">
                                {ticket.protocolo}
                              </span>
                              <span
                                className={`text-[10px] font-bold px-2 py-0.5 rounded border shrink-0 ${getPrioridadeBadgeClasses(
                                  ticket.prioridade
                                )}`}
                              >
                                {ticket.prioridade}
                              </span>
                            </div>

                            <h3 className="text-sm font-medium text-slate-100 line-clamp-2 leading-snug break-words">
                              {ticket.titulo}
                            </h3>

                            <div className="flex flex-wrap items-center gap-1.5 text-[11px]">
                              <span className="px-2 py-0.5 rounded bg-slate-900 text-slate-300 border border-slate-800">
                                {ticket.categoria}
                              </span>

                              {ticket.ativo && (
                                <button
                                  type="button"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    void abrirDiagnosticoAtivo(ticket.ativo!.id);
                                  }}
                                  className="flex items-center gap-1 px-2 py-0.5 rounded bg-indigo-500/15 text-indigo-300 border border-indigo-500/30 hover:bg-indigo-500/25 font-mono"
                                  title="Abrir Diagnóstico deste Ativo"
                                >
                                  <Tag className="w-3 h-3 shrink-0" />
                                  {ticket.ativo.tagPatrimonio}
                                </button>
                              )}
                            </div>

                            <div className="pt-2 border-t border-slate-900 flex items-center justify-between gap-2 text-[11px] text-slate-400">
                              <span className="truncate">
                                {ticket.colaborador.nome}
                              </span>
                              <span
                                className={`font-mono shrink-0 ${
                                  ticket.slaEstourado
                                    ? 'text-red-400 font-semibold'
                                    : 'text-slate-400'
                                }`}
                                title={`Prazo limite SLA: ${formatDateTime(
                                  ticket.dataLimiteSla
                                )}`}
                              >
                                SLA: {formatDateTime(ticket.dataLimiteSla)}
                              </span>
                            </div>
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* MÓDULO 2: INVENTÁRIO DE ATIVOS (Cards no celular sem scroll horizontal, Tabela no desktop) */}
        {activeTab === 'inventario' && (
          <div className="space-y-4">
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-3 sm:p-4 flex flex-col lg:flex-row lg:items-center lg:justify-between gap-3">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 flex-1">
                <div className="relative sm:col-span-1">
                  <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    aria-label="Buscar ativo por Tag, IP ou Colaborador"
                    value={buscaAtivo}
                    onChange={(e) => setBuscaAtivo(e.target.value)}
                    placeholder="Tag (TI-00142), IP ou Colaborador..."
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3 py-2 text-xs sm:text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-blue-500"
                  />
                </div>

                <select
                  aria-label="Filtrar por status do ativo"
                  value={filtroStatusAtivo}
                  onChange={(e) => setFiltroStatusAtivo(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs sm:text-sm text-slate-200"
                >
                  <option value="">Todos os Status</option>
                  <option value="EM_USO">Em Uso</option>
                  <option value="DISPONIVEL">Disponível</option>
                  <option value="EM_MANUTENCAO">Em Manutenção</option>
                  <option value="DESCARTADO">Descartado</option>
                </select>

                <select
                  aria-label="Filtrar por tipo do ativo"
                  value={filtroTipoAtivo}
                  onChange={(e) => setFiltroTipoAtivo(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs sm:text-sm text-slate-200"
                >
                  <option value="">Todos os Tipos</option>
                  <option value="NOTEBOOK">Notebook</option>
                  <option value="DESKTOP">Desktop</option>
                  <option value="MONITOR">Monitor</option>
                  <option value="IMPRESSORA">Impressora</option>
                  <option value="SWITCH">Switch</option>
                  <option value="ROTEADOR">Roteador</option>
                </select>
              </div>

              <button
                type="button"
                onClick={() => setShowNovoAtivoModal(true)}
                className="w-full lg:w-auto flex items-center justify-center gap-1.5 px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs sm:text-sm font-medium transition shrink-0"
              >
                <Plus className="w-4 h-4" />
                Cadastrar Ativo
              </button>
            </div>

            {/* Visão Mobile (Cards responsivos verticais) e Desktop (Tabela) em layout unificado sem scroll horizontal */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
              <div className="divide-y divide-slate-800">
                {/* Cabeçalho visível apenas em telas médias/grandes */}
                <div className="hidden md:grid md:grid-cols-12 gap-3 px-4 py-3 bg-slate-950/60 text-xs uppercase text-slate-400 font-semibold">
                  <div className="col-span-2">Tag Patrimônio</div>
                  <div className="col-span-2">Equipamento</div>
                  <div className="col-span-3">Hardware (CPU / RAM / Disco)</div>
                  <div className="col-span-2">Rede (IP / MAC)</div>
                  <div className="col-span-1">Status</div>
                  <div className="col-span-2 text-right">Responsável / Ação</div>
                </div>

                {ativosFiltrados.map((ativo) => (
                  <div
                    key={ativo.id}
                    className="p-3.5 sm:p-4 hover:bg-slate-800/40 transition flex flex-col md:grid md:grid-cols-12 gap-2.5 md:gap-3 md:items-center text-sm"
                  >
                    {/* Linha topo no mobile: Tag + Status */}
                    <div className="flex items-center justify-between md:col-span-2">
                      <span className="font-mono font-bold text-blue-400 text-sm">
                        {ativo.tagPatrimonio}
                      </span>
                      <span
                        className={`md:hidden text-[11px] font-semibold px-2 py-0.5 rounded border ${getStatusAtivoBadgeClasses(
                          ativo.status
                        )}`}
                      >
                        {ativo.status}
                      </span>
                    </div>

                    {/* Equipamento */}
                    <div className="md:col-span-2 min-w-0">
                      <div className="font-medium text-white truncate">
                        {ativo.fabricante} {ativo.modelo}
                      </div>
                      <div className="text-xs text-slate-400 truncate">
                        {ativo.tipo} • S/N: {ativo.numeroSerie || 'N/A'}
                      </div>
                    </div>

                    {/* Hardware */}
                    <div className="md:col-span-3 text-xs text-slate-300 min-w-0">
                      <div className="truncate">{ativo.processador || '—'}</div>
                      <div className="text-slate-400 truncate">
                        {ativo.memoriaRam || '—'} • {ativo.armazenamento || '—'}
                      </div>
                    </div>

                    {/* Rede (IP / MAC) */}
                    <div className="md:col-span-2 font-mono text-xs flex md:block items-center justify-between bg-slate-950/60 md:bg-transparent px-2.5 py-1.5 md:p-0 rounded-lg">
                      <div className="text-emerald-300">
                        IP: {ativo.ipLocal || 'DHCP'}
                      </div>
                      <div className="text-slate-500 text-[11px]">
                        {ativo.enderecoMac || '—'}
                      </div>
                    </div>

                    {/* Status Badge no Desktop */}
                    <div className="hidden md:block md:col-span-1">
                      <span
                        className={`text-[11px] font-semibold px-2 py-0.5 rounded border ${getStatusAtivoBadgeClasses(
                          ativo.status
                        )}`}
                      >
                        {ativo.status}
                      </span>
                    </div>

                    {/* Colaborador + Botão Ver Diagnóstico */}
                    <div className="md:col-span-2 flex items-center justify-between md:justify-end gap-2 pt-1 md:pt-0 border-t border-slate-800/70 md:border-0">
                      <div className="text-xs min-w-0 md:text-right">
                        {ativo.colaborador ? (
                          <div className="truncate">
                            <span className="font-medium text-slate-200">
                              {ativo.colaborador.nome}
                            </span>
                            <span className="block text-[11px] text-slate-400">
                              {ativo.colaborador.departamento}
                            </span>
                          </div>
                        ) : (
                          <span className="text-slate-500 italic">
                            Estoque TI
                          </span>
                        )}
                      </div>

                      <button
                        type="button"
                        onClick={() => void abrirDiagnosticoAtivo(ativo.id)}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-600/20 hover:bg-blue-600/30 text-blue-300 border border-blue-500/30 text-xs font-medium transition shrink-0"
                      >
                        <Cpu className="w-3.5 h-3.5" />
                        <span>Diagnóstico</span>
                      </button>
                    </div>
                  </div>
                ))}

                {ativosFiltrados.length === 0 && (
                  <div className="py-8 text-center text-slate-400 text-sm">
                    Nenhum ativo encontrado para o filtro informado.
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* MÓDULO 3: TELA DE DIAGNÓSTICO DO ATIVO */}
        {activeTab === 'diagnostico' && (
          <div className="space-y-4 sm:space-y-6">
            {/* Seletor rápido de Ativo para Diagnóstico */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-3.5 sm:p-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              <div className="flex items-center gap-2.5 min-w-0">
                <Monitor className="w-5 h-5 text-blue-400 shrink-0" />
                <div className="min-w-0">
                  <h2 className="text-sm font-semibold text-white truncate">
                    Diagnóstico Patrimonial & Histórico de Falhas
                  </h2>
                  <p className="text-xs text-slate-400 truncate">
                    Hardware, conectividade e incidentes vinculados à máquina.
                  </p>
                </div>
              </div>

              <div className="flex flex-col sm:flex-row sm:items-center gap-1.5 sm:gap-2 w-full sm:w-auto min-w-0">
                <label
                  htmlFor="select-ativo-diagnostico"
                  className="text-xs text-slate-400 shrink-0"
                >
                  Equipamento:
                </label>
                <select
                  id="select-ativo-diagnostico"
                  value={ativoDiagnostico?.ativo.id || ''}
                  onChange={(e) => {
                    if (e.target.value) {
                      void abrirDiagnosticoAtivo(e.target.value);
                    }
                  }}
                  className="w-full sm:w-auto max-w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs sm:text-sm text-slate-100 font-mono truncate"
                >
                  {ativos.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.tagPatrimonio} — {a.fabricante} {a.modelo} ({a.status})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {loadingDiagnostico ? (
              <div className="p-12 text-center text-slate-400 text-sm">
                Carregando telemetria e histórico do ativo...
              </div>
            ) : ativoDiagnostico ? (
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 sm:gap-6">
                {/* Coluna 1: Ficha Técnica do Hardware */}
                <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 sm:p-5 space-y-4 sm:space-y-5 min-w-0">
                  <div className="flex items-start justify-between gap-2 border-b border-slate-800 pb-3.5">
                    <div className="min-w-0">
                      <span className="text-xs font-mono text-blue-400 font-bold">
                        {ativoDiagnostico.ativo.tagPatrimonio}
                      </span>
                      <h3 className="text-base sm:text-lg font-bold text-white mt-0.5 break-words">
                        {ativoDiagnostico.ativo.fabricante}{' '}
                        {ativoDiagnostico.ativo.modelo}
                      </h3>
                      <p className="text-xs text-slate-400 break-words">
                        {ativoDiagnostico.ativo.tipo} • S/N:{' '}
                        {ativoDiagnostico.ativo.numeroSerie || 'N/A'}
                      </p>
                    </div>
                    <span
                      className={`text-[11px] font-semibold px-2.5 py-1 rounded border shrink-0 ${getStatusAtivoBadgeClasses(
                        ativoDiagnostico.ativo.status
                      )}`}
                    >
                      {ativoDiagnostico.ativo.status}
                    </span>
                  </div>

                  <div className="space-y-2.5 text-xs sm:text-sm">
                    <div className="flex flex-wrap items-center justify-between gap-2 py-2 border-b border-slate-800/60">
                      <span className="text-slate-400 flex items-center gap-2">
                        <Cpu className="w-4 h-4 text-blue-400 shrink-0" /> Processador
                      </span>
                      <span className="font-medium text-slate-200 text-right break-words">
                        {ativoDiagnostico.ativo.processador || 'Não aplicável'}
                      </span>
                    </div>
                    <div className="flex flex-wrap items-center justify-between gap-2 py-2 border-b border-slate-800/60">
                      <span className="text-slate-400 flex items-center gap-2">
                        <Server className="w-4 h-4 text-purple-400 shrink-0" /> Memória RAM
                      </span>
                      <span className="font-medium text-slate-200 text-right">
                        {ativoDiagnostico.ativo.memoriaRam || 'Não aplicável'}
                      </span>
                    </div>
                    <div className="flex flex-wrap items-center justify-between gap-2 py-2 border-b border-slate-800/60">
                      <span className="text-slate-400 flex items-center gap-2">
                        <HardDrive className="w-4 h-4 text-emerald-400 shrink-0" /> Armazenamento
                      </span>
                      <span className="font-medium text-slate-200 text-right">
                        {ativoDiagnostico.ativo.armazenamento || 'Não aplicável'}
                      </span>
                    </div>
                    <div className="flex flex-wrap items-center justify-between gap-2 py-2 border-b border-slate-800/60">
                      <span className="text-slate-400 flex items-center gap-2">
                        <Monitor className="w-4 h-4 text-amber-400 shrink-0" /> Sistema Operacional
                      </span>
                      <span className="font-medium text-slate-200 text-right">
                        {ativoDiagnostico.ativo.sistemaOperacional || 'N/A'}
                      </span>
                    </div>
                    <div className="flex flex-wrap items-center justify-between gap-2 py-2 border-b border-slate-800/60">
                      <span className="text-slate-400 flex items-center gap-2">
                        <Network className="w-4 h-4 text-cyan-400 shrink-0" /> IP / MAC
                      </span>
                      <span className="font-mono text-xs text-emerald-300 text-right break-all">
                        {ativoDiagnostico.ativo.ipLocal || 'DHCP'} (
                        {ativoDiagnostico.ativo.enderecoMac || '—'})
                      </span>
                    </div>
                    <div className="flex flex-wrap items-center justify-between gap-2 py-2">
                      <span className="text-slate-400 flex items-center gap-2">
                        <User className="w-4 h-4 text-indigo-400 shrink-0" /> Responsável
                      </span>
                      <span className="font-medium text-slate-200 text-right">
                        {ativoDiagnostico.ativo.colaborador
                          ? `${ativoDiagnostico.ativo.colaborador.nome} (${ativoDiagnostico.ativo.colaborador.departamento})`
                          : 'Estoque TI'}
                      </span>
                    </div>
                  </div>

                  <div className="p-3 sm:p-3.5 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <ShieldAlert className="w-5 h-5 text-orange-400 shrink-0" />
                      <div>
                        <div className="text-xs font-semibold text-white">
                          Incidentes de Hardware
                        </div>
                        <div className="text-[11px] text-slate-400">
                          Total acumulado no ciclo de vida
                        </div>
                      </div>
                    </div>
                    <span className="text-xl font-bold font-mono text-orange-400">
                      {ativoDiagnostico.totalIncidenteHardware}
                    </span>
                  </div>
                </div>

                {/* Coluna 2 e 3: Chamados Atrelados à Máquina + Histórico Patrimonial */}
                <div className="lg:col-span-2 space-y-4 sm:space-y-6 min-w-0">
                  {/* Chamados Vinculados ao Ativo */}
                  <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 sm:p-5">
                    <h3 className="text-sm sm:text-base font-semibold text-white mb-3 sm:mb-4">
                      Chamados Vinculados a este Equipamento (
                      {ativoDiagnostico.chamadosVinculados.length})
                    </h3>

                    {ativoDiagnostico.chamadosVinculados.length === 0 ? (
                      <p className="text-sm text-slate-400 py-4">
                        Nenhum incidente registrado para este ativo até o momento.
                      </p>
                    ) : (
                      <div className="space-y-3">
                        {ativoDiagnostico.chamadosVinculados.map((t) => (
                          <div
                            key={t.id}
                            onClick={() => void abrirDetalhesTicket(t)}
                            className="p-3 sm:p-3.5 rounded-xl bg-slate-950 border border-slate-800 hover:border-blue-500/50 cursor-pointer transition flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2.5"
                          >
                            <div className="space-y-1 min-w-0">
                              <div className="flex flex-wrap items-center gap-1.5">
                                <span className="font-mono text-xs font-bold text-blue-400">
                                  {t.protocolo}
                                </span>
                                <span
                                  className={`text-[10px] font-bold px-2 py-0.5 rounded border ${getPrioridadeBadgeClasses(
                                    t.prioridade
                                  )}`}
                                >
                                  {t.prioridade}
                                </span>
                                <span className="text-[11px] px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-slate-300">
                                  {t.status}
                                </span>
                              </div>
                              <p className="text-sm font-medium text-white break-words">
                                {t.titulo}
                              </p>
                              <p className="text-xs text-slate-400">
                                Solicitante: {t.colaborador.nome} • Técnico:{' '}
                                {t.tecnicoAtribuido?.nome || 'Não atribuído'}
                              </p>
                            </div>
                            <div className="sm:text-right text-[11px] text-slate-400 font-mono shrink-0">
                              <div>Abertura: {formatDateTime(t.dataAbertura)}</div>
                              <div>Limite SLA: {formatDateTime(t.dataLimiteSla)}</div>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Log de Auditoria Patrimonial (historico_ativos) */}
                  <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 sm:p-5">
                    <h3 className="text-sm sm:text-base font-semibold text-white mb-3 sm:mb-4">
                      Linha do Tempo de Auditoria Patrimonial (`historico_ativos`)
                    </h3>
                    <div className="space-y-2.5">
                      {ativoDiagnostico.historicoPatrimonial.map((h) => (
                        <div
                          key={h.id}
                          className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-1.5 sm:gap-4 p-3 rounded-lg bg-slate-950 border border-slate-800 text-xs"
                        >
                          <span className="text-slate-200 break-words">
                            {h.observacao}
                          </span>
                          <span className="font-mono text-slate-400 shrink-0">
                            {formatDateTime(h.dataRegistro)}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            ) : null}
          </div>
        )}
      </main>

      {/* MODAL DE DETALHES DO CHAMADO, TRANSIÇÃO DE ESTADO E TIMELINE DE NOTAS */}
      {selectedTicket && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-end sm:items-center justify-center p-2 sm:p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-3xl w-full max-h-[92vh] overflow-y-auto p-4 sm:p-6 space-y-4 sm:space-y-5">
            <div className="flex items-start justify-between gap-3 border-b border-slate-800 pb-3.5">
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-1.5">
                  <span className="font-mono text-xs sm:text-sm font-bold text-blue-400">
                    {selectedTicket.protocolo}
                  </span>
                  <span
                    className={`text-[11px] font-bold px-2 py-0.5 rounded border ${getPrioridadeBadgeClasses(
                      selectedTicket.prioridade
                    )}`}
                  >
                    {selectedTicket.prioridade} (SLA {selectedTicket.horasSlaPadrao}h)
                  </span>
                  <span className="text-[11px] px-2 py-0.5 rounded bg-slate-800 text-slate-200 font-semibold">
                    {selectedTicket.status}
                  </span>
                </div>
                <h2 className="text-base sm:text-lg font-bold text-white mt-2 break-words">
                  {selectedTicket.titulo}
                </h2>
              </div>
              <button
                type="button"
                onClick={() => setSelectedTicket(null)}
                className="p-1.5 rounded-lg bg-slate-800 text-slate-400 hover:text-white shrink-0"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="bg-slate-950 border border-slate-800 rounded-xl p-3.5 sm:p-4 text-xs sm:text-sm text-slate-200 space-y-2">
              <p className="break-words">{selectedTicket.descricao}</p>
              <div className="pt-2 border-t border-slate-900 flex flex-wrap items-center gap-3 text-xs text-slate-400">
                <span>
                  <strong>Solicitante:</strong> {selectedTicket.colaborador.nome} (
                  {selectedTicket.colaborador.departamento})
                </span>
                <span>
                  <strong>Ativo:</strong>{' '}
                  {selectedTicket.ativo
                    ? `${selectedTicket.ativo.tagPatrimonio} (${selectedTicket.ativo.modelo})`
                    : 'Sem ativo vinculado'}
                </span>
                <span>
                  <strong>Prazo SLA:</strong>{' '}
                  {formatDateTime(selectedTicket.dataLimiteSla)}
                </span>
              </div>
            </div>

            {/* Máquina de Estados do Chamado */}
            {TRANSICOES_VALIDAS[selectedTicket.status].length > 0 && (
              <div className="bg-slate-950 border border-slate-800 rounded-xl p-3.5 sm:p-4 space-y-3">
                <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                  Avançar Etapa (State Machine) & Atribuição N1/N2
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs text-slate-400 mb-1">
                      Técnico Responsável
                    </label>
                    <select
                      value={tecnicoTransicaoId}
                      onChange={(e) => setTecnicoTransicaoId(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs sm:text-sm text-slate-100"
                    >
                      <option value="">Selecione um técnico...</option>
                      {tecnicosDisponiveis.map((tec) => (
                        <option key={tec.id} value={tec.id}>
                          {tec.nome} ({tec.cargo})
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs text-slate-400 mb-1">
                      Observação da Transição (Opcional)
                    </label>
                    <input
                      type="text"
                      value={obsTransicao}
                      onChange={(e) => setObsTransicao(e.target.value)}
                      placeholder="Ex: Iniciado diagnóstico de hardware..."
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs sm:text-sm text-slate-100"
                    />
                  </div>
                </div>
                <div className="flex flex-wrap items-center gap-2 pt-1">
                  {TRANSICOES_VALIDAS[selectedTicket.status].map((proxStatus) => (
                    <button
                      key={proxStatus}
                      type="button"
                      onClick={() => void handleTransicaoTicket(proxStatus)}
                      className="w-full sm:w-auto px-3.5 py-2 sm:py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold transition"
                    >
                      Transicionar para {proxStatus}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Timeline de Notas Públicas e Internas de TI */}
            <div className="space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="text-sm font-semibold text-white flex items-center gap-2">
                  <MessageSquare className="w-4 h-4 text-blue-400" />
                  Linha do Tempo ({ticketNotas.length})
                </h3>
                <label className="flex items-center gap-2 text-xs text-slate-300 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={incluirNotasPrivadas}
                    onChange={(e) => void alternarVisibilidadeNotas(e.target.checked)}
                    className="rounded border-slate-700"
                  />
                  Exibir Notas Internas de TI
                </label>
              </div>

              <div className="space-y-2.5 max-h-60 overflow-y-auto pr-1">
                {ticketNotas.map((nota) => (
                  <div
                    key={nota.id}
                    className={`p-3 rounded-xl border text-xs space-y-1 ${
                      nota.isPrivadaTi
                        ? 'bg-amber-500/10 border-amber-500/30 text-amber-100'
                        : 'bg-slate-950 border-slate-800 text-slate-200'
                    }`}
                  >
                    <div className="flex flex-wrap items-center justify-between gap-1">
                      <span className="font-semibold flex items-center gap-1.5">
                        {nota.autor.nome}
                        {nota.isPrivadaTi && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 text-[10px]">
                            <Lock className="w-3 h-3" /> Nota Interna TI
                          </span>
                        )}
                      </span>
                      <span className="font-mono text-slate-400">
                        {formatDateTime(nota.dataRegistro)}
                      </span>
                    </div>
                    <p className="break-words">{nota.conteudo}</p>
                  </div>
                ))}
              </div>

              {selectedTicket.status !== 'FECHADO' && (
                <form onSubmit={handleAdicionarNota} className="space-y-2 pt-2">
                  <textarea
                    rows={2}
                    value={novaNotaConteudo}
                    onChange={(e) => setNovaNotaConteudo(e.target.value)}
                    placeholder="Adicionar comentário público ou anotação técnica de debug..."
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-xs sm:text-sm text-slate-100"
                  />
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <label className="flex items-center gap-2 text-xs text-amber-300 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={novaNotaPrivada}
                        onChange={(e) => setNovaNotaPrivada(e.target.checked)}
                      />
                      Restrito à equipe de TI (`isPrivadaTi`)
                    </label>
                    <button
                      type="submit"
                      className="w-full sm:w-auto px-4 py-2 sm:py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold"
                    >
                      Registrar na Timeline
                    </button>
                  </div>
                </form>
              )}
            </div>
          </div>
        </div>
      )}

      {/* MODAL DE ABERTURA DE NOVO CHAMADO */}
      {showNovoTicketModal && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-end sm:items-center justify-center p-2 sm:p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full max-h-[92vh] overflow-y-auto p-4 sm:p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h2 className="text-base font-bold text-white">
                Abrir Novo Chamado Técnico
              </h2>
              <button
                type="button"
                onClick={() => setShowNovoTicketModal(false)}
                className="text-slate-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCriarTicket} className="space-y-3 text-sm">
              <div>
                <label className="block text-xs text-slate-400 mb-1">
                  Título do Incidente / Solicitação
                </label>
                <input
                  required
                  type="text"
                  placeholder="Ex: Falha de autenticação na rede Wi-Fi corporativa"
                  value={novoTicketForm.titulo}
                  onChange={(e) =>
                    setNovoTicketForm({ ...novoTicketForm, titulo: e.target.value })
                  }
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-slate-100"
                />
              </div>

              <div>
                <label className="block text-xs text-slate-400 mb-1">
                  Descrição Detalhada
                </label>
                <textarea
                  required
                  rows={3}
                  placeholder="Descreva os sintomas, mensagens de erro e impacto..."
                  value={novoTicketForm.descricao}
                  onChange={(e) =>
                    setNovoTicketForm({
                      ...novoTicketForm,
                      descricao: e.target.value,
                    })
                  }
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-slate-100"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">
                    Categoria
                  </label>
                  <select
                    value={novoTicketForm.categoria}
                    onChange={(e) =>
                      setNovoTicketForm({
                        ...novoTicketForm,
                        categoria: e.target.value as CategoriaTicket,
                      })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-slate-100"
                  >
                    <option value="HARDWARE">HARDWARE</option>
                    <option value="SOFTWARE">SOFTWARE</option>
                    <option value="REDE_ACESSO">REDE_ACESSO</option>
                    <option value="TELEFONIA">TELEFONIA</option>
                    <option value="DUVIDA_GERAL">DUVIDA_GERAL</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs text-slate-400 mb-1">
                    Prioridade / Severidade (SLA)
                  </label>
                  <select
                    value={novoTicketForm.prioridade}
                    onChange={(e) =>
                      setNovoTicketForm({
                        ...novoTicketForm,
                        prioridade: e.target.value as PrioridadeTicket,
                      })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-slate-100"
                  >
                    <option value="CRITICA">CRÍTICA (+2h SLA)</option>
                    <option value="ALTA">ALTA (+8h SLA)</option>
                    <option value="MEDIA">MÉDIA (+24h SLA)</option>
                    <option value="BAIXA">BAIXA (+48h SLA)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs text-slate-400 mb-1">
                  Colaborador Solicitante
                </label>
                <select
                  required
                  value={novoTicketForm.colaboradorId}
                  onChange={(e) =>
                    setNovoTicketForm({
                      ...novoTicketForm,
                      colaboradorId: e.target.value,
                    })
                  }
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-slate-100"
                >
                  {colaboradores.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.nome} ({c.departamento})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs text-slate-400 mb-1">
                  Ativo Patrimonial Vinculado (Opcional)
                </label>
                <select
                  value={novoTicketForm.ativoId}
                  onChange={(e) =>
                    setNovoTicketForm({
                      ...novoTicketForm,
                      ativoId: e.target.value,
                    })
                  }
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-slate-100"
                >
                  <option value="">Sem vínculo de equipamento</option>
                  {ativos
                    .filter((a) => a.status !== 'DESCARTADO')
                    .map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.tagPatrimonio} — {a.fabricante} {a.modelo}
                      </option>
                    ))}
                </select>
              </div>

              <div className="flex justify-end gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setShowNovoTicketModal(false)}
                  className="px-4 py-2 rounded-lg bg-slate-800 text-slate-300 text-xs font-medium"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold"
                >
                  Gerar Protocolo & Abrir Chamado
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL DE CADASTRO DE ATIVO */}
      {showNovoAtivoModal && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-end sm:items-center justify-center p-2 sm:p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full max-h-[92vh] overflow-y-auto p-4 sm:p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h2 className="text-base font-bold text-white">
                Cadastrar Equipamento no Inventário (ITAM)
              </h2>
              <button
                type="button"
                onClick={() => setShowNovoAtivoModal(false)}
                className="text-slate-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCriarAtivo} className="space-y-3 text-sm">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">
                    Tag Patrimônio (TI-XXXXX)
                  </label>
                  <input
                    required
                    type="text"
                    value={novoAtivoForm.tagPatrimonio}
                    onChange={(e) =>
                      setNovoAtivoForm({
                        ...novoAtivoForm,
                        tagPatrimonio: e.target.value,
                      })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-slate-100 font-mono"
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">
                    Categoria
                  </label>
                  <select
                    value={novoAtivoForm.tipo}
                    onChange={(e) =>
                      setNovoAtivoForm({
                        ...novoAtivoForm,
                        tipo: e.target.value as TipoAtivo,
                      })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-slate-100"
                  >
                    <option value="NOTEBOOK">NOTEBOOK</option>
                    <option value="DESKTOP">DESKTOP</option>
                    <option value="MONITOR">MONITOR</option>
                    <option value="IMPRESSORA">IMPRESSORA</option>
                    <option value="SWITCH">SWITCH</option>
                    <option value="ROTEADOR">ROTEADOR</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">
                    Fabricante
                  </label>
                  <input
                    required
                    type="text"
                    value={novoAtivoForm.fabricante}
                    onChange={(e) =>
                      setNovoAtivoForm({
                        ...novoAtivoForm,
                        fabricante: e.target.value,
                      })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">
                    Modelo
                  </label>
                  <input
                    required
                    type="text"
                    value={novoAtivoForm.modelo}
                    onChange={(e) =>
                      setNovoAtivoForm({
                        ...novoAtivoForm,
                        modelo: e.target.value,
                      })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-slate-100"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">
                    Endereço IP Local
                  </label>
                  <input
                    type="text"
                    value={novoAtivoForm.ipLocal}
                    onChange={(e) =>
                      setNovoAtivoForm({
                        ...novoAtivoForm,
                        ipLocal: e.target.value,
                      })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-slate-100 font-mono"
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">
                    Colaborador Responsável
                  </label>
                  <select
                    value={novoAtivoForm.colaboradorId}
                    onChange={(e) =>
                      setNovoAtivoForm({
                        ...novoAtivoForm,
                        colaboradorId: e.target.value,
                      })
                    }
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-slate-100"
                  >
                    <option value="">Estoque TI (Disponível)</option>
                    {colaboradores.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.nome} ({c.departamento})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setShowNovoAtivoModal(false)}
                  className="px-4 py-2 rounded-lg bg-slate-800 text-slate-300 text-xs font-medium"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold"
                >
                  Salvar Ativo
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
