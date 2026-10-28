export type PerfilUsuario = 'COLABORADOR' | 'TECNICO' | 'ADMIN';

export type TipoAtivo =
  | 'DESKTOP'
  | 'NOTEBOOK'
  | 'MONITOR'
  | 'IMPRESSORA'
  | 'SWITCH'
  | 'ROTEADOR'
  | 'OUTRO';

export type StatusAtivo =
  | 'DISPONIVEL'
  | 'EM_USO'
  | 'EM_MANUTENCAO'
  | 'DESCARTADO';

export type CategoriaTicket =
  | 'HARDWARE'
  | 'SOFTWARE'
  | 'REDE_ACESSO'
  | 'TELEFONIA'
  | 'DUVIDA_GERAL';

export type PrioridadeTicket = 'CRITICA' | 'ALTA' | 'MEDIA' | 'BAIXA';

export type StatusTicket =
  | 'ABERTO'
  | 'EM_ATENDIMENTO'
  | 'AGUARDANDO_USUARIO'
  | 'AGUARDANDO_PECA'
  | 'RESOLVIDO'
  | 'FECHADO';

export interface Colaborador {
  id: string;
  nome: string;
  email: string;
  cargo: string;
  departamento: string;
  perfil: PerfilUsuario;
}

export interface Ativo {
  id: string;
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
  status: StatusAtivo;
  colaborador?: Colaborador | null;
}

export interface HistoricoAtivo {
  id: string;
  ativoId: string;
  tagPatrimonio: string;
  observacao: string;
  dataRegistro: string;
}

export interface Ticket {
  id: string;
  protocolo: string;
  titulo: string;
  descricao: string;
  categoria: CategoriaTicket;
  prioridade: PrioridadeTicket;
  status: StatusTicket;
  colaborador: Colaborador;
  tecnicoAtribuido?: Colaborador | null;
  ativo?: Ativo | null;
  dataAbertura: string;
  dataLimiteSla: string;
  dataFechamento?: string | null;
  slaEstourado: boolean;
  horasSlaPadrao: number;
}

export interface TicketNota {
  id: string;
  ticketId: string;
  autor: Colaborador;
  conteudo: string;
  isPrivadaTi: boolean;
  dataRegistro: string;
}

export interface AtivoDiagnostico {
  ativo: Ativo;
  totalIncidenteHardware: number;
  chamadosVinculados: Ticket[];
  historicoPatrimonial: HistoricoAtivo[];
}

export interface DashboardMetricas {
  totalTickets: number;
  ticketsAbertos: number;
  ticketsEmAtendimento: number;
  ticketsAguardando: number;
  ticketsResolvidosOuFechados: number;
  ticketsCriticosAtivos: number;
  ticketsSlaViolado: number;
  taxaCumprimentoSlaPercentual: number;
  mttrHorasMedia: number;
  totalAtivos: number;
  ativosEmUso: number;
  ativosDisponiveis: number;
  ativosEmManutencao: number;
  ticketsPorStatus: Record<string, number>;
  ticketsPorPrioridade: Record<string, number>;
  ticketsPorCategoria: Record<string, number>;
}
