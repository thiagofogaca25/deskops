import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App, { getPrioridadeBadgeClasses, getStatusAtivoBadgeClasses } from './App';
import { deskopsApi } from './services/api';
import type {
  Ativo,
  AtivoDiagnostico,
  Colaborador,
  DashboardMetricas,
  Ticket,
} from './types/deskops';

vi.mock('./services/api', () => ({
  deskopsApi: {
    getMetricas: vi.fn(),
    getHealth: vi.fn(),
    getColaboradores: vi.fn(),
    getAtivos: vi.fn(),
    getAtivoDiagnostico: vi.fn(),
    getTickets: vi.fn(),
    getTicketNotas: vi.fn(),
    abrirTicket: vi.fn(),
    atualizarStatusTicket: vi.fn(),
    adicionarTicketNota: vi.fn(),
    criarAtivo: vi.fn(),
  },
}));

const mockColaboradores: Colaborador[] = [
  {
    id: 'c1',
    nome: 'Mariana Costa',
    email: 'mariana.costa@deskops.local',
    cargo: 'Analista Financeiro Sênior',
    departamento: 'Financeiro',
    perfil: 'COLABORADOR',
  },
  {
    id: 'c2',
    nome: 'Fernanda Lima (Field Service N2)',
    email: 'fernanda.lima@deskops.local',
    cargo: 'Analista de Infraestrutura N2',
    departamento: 'Operações TI',
    perfil: 'TECNICO',
  },
];

const mockAtivos: Ativo[] = [
  {
    id: 'a1',
    tagPatrimonio: 'TI-00142',
    tipo: 'NOTEBOOK',
    fabricante: 'Dell',
    modelo: 'Latitude 5440',
    numeroSerie: 'BR-DL5440-9981',
    processador: 'Intel Core i7-1355U',
    memoriaRam: '16 GB DDR4',
    armazenamento: '512 GB NVMe SSD',
    sistemaOperacional: 'Windows 11 Pro 23H2',
    enderecoMac: '00:1B:44:11:3A:B7',
    ipLocal: '192.168.10.45',
    status: 'EM_USO',
    colaborador: mockColaboradores[0],
  },
  {
    id: 'a2',
    tagPatrimonio: 'TI-00201',
    tipo: 'SWITCH',
    fabricante: 'Cisco',
    modelo: 'Catalyst 2960-X',
    numeroSerie: 'FOC2314X09A',
    processador: 'ARM Cortex-A9',
    memoriaRam: '512 MB',
    armazenamento: '128 MB Flash',
    sistemaOperacional: 'Cisco IOS 15.2',
    enderecoMac: '00:25:90:AA:10:01',
    ipLocal: '192.168.1.2',
    status: 'EM_MANUTENCAO',
    colaborador: mockColaboradores[1],
  },
];

const mockTickets: Ticket[] = [
  {
    id: 't1',
    protocolo: 'TKT-202610-0001',
    titulo: 'Queda de link VLAN Financeiro no Switch do 2º Andar',
    descricao: 'Perda de pacotes ao acessar o ERP.',
    categoria: 'REDE_ACESSO',
    prioridade: 'CRITICA',
    status: 'ABERTO',
    colaborador: mockColaboradores[0],
    tecnicoAtribuido: null,
    ativo: mockAtivos[1],
    dataAbertura: '2026-10-27T10:00:00',
    dataLimiteSla: '2026-10-27T12:00:00',
    dataFechamento: null,
    slaEstourado: false,
    horasSlaPadrao: 2,
  },
  {
    id: 't2',
    protocolo: 'TKT-202610-0002',
    titulo: 'Falha de vídeo na porta USB-C do Latitude 5440',
    descricao: 'Monitor externo piscando.',
    categoria: 'HARDWARE',
    prioridade: 'ALTA',
    status: 'EM_ATENDIMENTO',
    colaborador: mockColaboradores[0],
    tecnicoAtribuido: mockColaboradores[1],
    ativo: mockAtivos[0],
    dataAbertura: '2026-10-27T09:00:00',
    dataLimiteSla: '2026-10-27T17:00:00',
    dataFechamento: null,
    slaEstourado: false,
    horasSlaPadrao: 8,
  },
];

const mockMetricas: DashboardMetricas = {
  totalTickets: 2,
  ticketsAbertos: 1,
  ticketsEmAtendimento: 1,
  ticketsAguardando: 0,
  ticketsResolvidosOuFechados: 0,
  ticketsCriticosAtivos: 1,
  ticketsSlaViolado: 0,
  taxaCumprimentoSlaPercentual: 100.0,
  mttrHorasMedia: 3.5,
  totalAtivos: 2,
  ativosEmUso: 1,
  ativosDisponiveis: 0,
  ativosEmManutencao: 1,
  ticketsPorStatus: { ABERTO: 1, EM_ATENDIMENTO: 1 },
  ticketsPorPrioridade: { CRITICA: 1, ALTA: 1 },
  ticketsPorCategoria: { REDE_ACESSO: 1, HARDWARE: 1 },
};

const mockDiagnostico: AtivoDiagnostico = {
  ativo: mockAtivos[0],
  totalIncidenteHardware: 1,
  chamadosVinculados: [mockTickets[1]],
  historicoPatrimonial: [
    {
      id: 'h1',
      ativoId: 'a1',
      tagPatrimonio: 'TI-00142',
      observacao: 'Entregue para Mariana Costa (Financeiro).',
      dataRegistro: '2026-10-20T09:00:00',
    },
  ],
};

describe('DeskOps Frontend Dashboard — Kanban, Inventário e Diagnóstico', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(deskopsApi.getMetricas).mockResolvedValue(mockMetricas);
    vi.mocked(deskopsApi.getHealth).mockResolvedValue({ status: 'UP' });
    vi.mocked(deskopsApi.getColaboradores).mockResolvedValue(mockColaboradores);
    vi.mocked(deskopsApi.getAtivos).mockResolvedValue(mockAtivos);
    vi.mocked(deskopsApi.getTickets).mockResolvedValue(mockTickets);
    vi.mocked(deskopsApi.getAtivoDiagnostico).mockResolvedValue(mockDiagnostico);
    vi.mocked(deskopsApi.getTicketNotas).mockResolvedValue([
      {
        id: 'n1',
        ticketId: 't1',
        autor: mockColaboradores[1],
        conteudo: '[NOTA INTERNA TI] Reiniciado porta Gi1/0/24 no switch.',
        isPrivadaTi: true,
        dataRegistro: '2026-10-27T10:15:00',
      },
    ]);
  });

  it('deve aplicar classes semânticas corretas para prioridades de chamado e status de ativo', () => {
    expect(getPrioridadeBadgeClasses('CRITICA')).toContain('text-red-300');
    expect(getPrioridadeBadgeClasses('ALTA')).toContain('text-orange-300');
    expect(getPrioridadeBadgeClasses('MEDIA')).toContain('text-amber-300');
    expect(getPrioridadeBadgeClasses('BAIXA')).toContain('text-emerald-300');

    expect(getStatusAtivoBadgeClasses('DISPONIVEL')).toContain('text-emerald-300');
    expect(getStatusAtivoBadgeClasses('EM_USO')).toContain('text-blue-300');
    expect(getStatusAtivoBadgeClasses('EM_MANUTENCAO')).toContain('text-amber-300');
    expect(getStatusAtivoBadgeClasses('DESCARTADO')).toContain('text-rose-300');
  });

  it('deve renderizar o Kanban de Chamados com protocolos, badges e métricas de SLA', async () => {
    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('TKT-202610-0001')).toBeInTheDocument();
    });

    expect(
      screen.getByText('Queda de link VLAN Financeiro no Switch do 2º Andar')
    ).toBeInTheDocument();
    expect(screen.getByText('TKT-202610-0002')).toBeInTheDocument();
    expect(screen.getByText('100%')).toBeInTheDocument();
    expect(screen.getByText('3.5h')).toBeInTheDocument();
  });

  it('deve navegar para a Tabela de Inventário e filtrar instantaneamente por Tag, IP ou Colaborador', async () => {
    const user = userEvent.setup();
    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('TKT-202610-0001')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /Inventário de Ativos/i }));

    expect(screen.getByText('TI-00142')).toBeInTheDocument();
    expect(screen.getByText('TI-00201')).toBeInTheDocument();

    const searchInput = screen.getByLabelText(
      /Buscar ativo por Tag, IP ou Colaborador/i
    );
    await user.type(searchInput, '192.168.10.45');

    expect(screen.getByText('TI-00142')).toBeInTheDocument();
    expect(screen.queryByText('TI-00201')).not.toBeInTheDocument();
  });

  it('deve exibir a Tela de Diagnóstico do Ativo com especificações de hardware, chamados atrelados e histórico', async () => {
    const user = userEvent.setup();
    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('TKT-202610-0001')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /Diagnóstico do Ativo/i }));

    await waitFor(() => {
      expect(screen.getByText('Intel Core i7-1355U')).toBeInTheDocument();
    });

    expect(screen.getByText('16 GB DDR4')).toBeInTheDocument();
    expect(screen.getByText('512 GB NVMe SSD')).toBeInTheDocument();
    expect(
      screen.getByText('Falha de vídeo na porta USB-C do Latitude 5440')
    ).toBeInTheDocument();
    expect(
      screen.getByText('Entregue para Mariana Costa (Financeiro).')
    ).toBeInTheDocument();
  });

  it('deve abrir o modal de detalhes do chamado e exibir a Linha do Tempo com notas de TI', async () => {
    const user = userEvent.setup();
    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('TKT-202610-0001')).toBeInTheDocument();
    });

    await user.click(screen.getByText('TKT-202610-0001'));

    await waitFor(() => {
      expect(
        screen.getByText('[NOTA INTERNA TI] Reiniciado porta Gi1/0/24 no switch.')
      ).toBeInTheDocument();
    });

    expect(screen.getByText('Nota Interna TI')).toBeInTheDocument();
  });
});
