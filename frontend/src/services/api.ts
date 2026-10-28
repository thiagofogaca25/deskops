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
} from '../types/deskops';

const API_BASE = import.meta.env.VITE_API_URL || 'http://192.168.1.6:8081';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options?.headers || {}),
    },
    ...options,
  });

  if (!response.ok) {
    let errorDetail = `Erro HTTP ${response.status}`;
    try {
      const problem = await response.json();
      if (problem.detail) {
        errorDetail = problem.detail;
      }
      if (problem.errors) {
        const msgs = Object.values(problem.errors).join(', ');
        errorDetail = `${errorDetail} (${msgs})`;
      }
    } catch {
      // ignore json parse error
    }
    throw new Error(errorDetail);
  }

  return response.json() as Promise<T>;
}

export const deskopsApi = {
  getMetricas: () => request<DashboardMetricas>('/api/dashboard/metricas'),

  getHealth: () =>
    request<{ status: string }>('/actuator/health').catch(() => ({
      status: 'UP',
    })),

  getColaboradores: () => request<Colaborador[]>('/api/colaboradores'),

  getAtivos: (params?: {
    status?: StatusAtivo;
    tipo?: TipoAtivo;
    busca?: string;
  }) => {
    const query = new URLSearchParams();
    if (params?.status) query.set('status', params.status);
    if (params?.tipo) query.set('tipo', params.tipo);
    if (params?.busca) query.set('busca', params.busca);
    const qs = query.toString();
    return request<Ativo[]>(`/api/ativos${qs ? `?${qs}` : ''}`);
  },

  getAtivoDiagnostico: (id: string) =>
    request<AtivoDiagnostico>(`/api/ativos/${id}/diagnostico`),

  criarAtivo: (payload: {
    tagPatrimonio: string;
    tipo: TipoAtivo;
    fabricante: string;
    modelo: string;
    numeroSerie?: string;
    processador?: string;
    memoriaRam?: string;
    armazenamento?: string;
    sistemaOperacional?: string;
    enderecoMac?: string;
    ipLocal?: string;
    status?: StatusAtivo;
    colaboradorId?: string | null;
  }) =>
    request<Ativo>('/api/ativos', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  atualizarStatusAtivo: (
    id: string,
    payload: {
      status: StatusAtivo;
      colaboradorId?: string | null;
      observacao?: string;
    }
  ) =>
    request<Ativo>(`/api/ativos/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),

  getTickets: () => request<Ticket[]>('/api/tickets'),

  abrirTicket: (payload: {
    titulo: string;
    descricao: string;
    categoria: CategoriaTicket;
    prioridade: PrioridadeTicket;
    colaboradorId: string;
    ativoId?: string | null;
    tecnicoAtribuidoId?: string | null;
  }) =>
    request<Ticket>('/api/tickets', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  atualizarStatusTicket: (
    id: string,
    payload: {
      status: StatusTicket;
      tecnicoAtribuidoId?: string | null;
      observacaoTransicao?: string;
      notaPrivadaTi?: boolean;
    }
  ) =>
    request<Ticket>(`/api/tickets/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),

  getTicketNotas: (ticketId: string, incluirPrivadas = true) =>
    request<TicketNota[]>(
      `/api/tickets/${ticketId}/notas?incluirPrivadas=${incluirPrivadas}`
    ),

  adicionarTicketNota: (
    ticketId: string,
    payload: {
      autorId: string;
      conteudo: string;
      isPrivadaTi: boolean;
    }
  ) =>
    request<TicketNota>(`/api/tickets/${ticketId}/notas`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
};
