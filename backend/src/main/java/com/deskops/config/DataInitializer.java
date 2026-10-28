package com.deskops.config;

import com.deskops.domain.entity.Ativo;
import com.deskops.domain.entity.Colaborador;
import com.deskops.domain.entity.Ticket;
import com.deskops.domain.entity.TicketNota;
import com.deskops.domain.enums.*;
import com.deskops.repository.AtivoRepository;
import com.deskops.repository.ColaboradorRepository;
import com.deskops.repository.TicketNotaRepository;
import com.deskops.repository.TicketRepository;
import com.deskops.service.AtivoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "deskops.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final ColaboradorRepository colaboradorRepository;
    private final AtivoRepository ativoRepository;
    private final TicketRepository ticketRepository;
    private final TicketNotaRepository ticketNotaRepository;
    private final AtivoService ativoService;

    public DataInitializer(
            ColaboradorRepository colaboradorRepository,
            AtivoRepository ativoRepository,
            TicketRepository ticketRepository,
            TicketNotaRepository ticketNotaRepository,
            AtivoService ativoService
    ) {
        this.colaboradorRepository = colaboradorRepository;
        this.ativoRepository = ativoRepository;
        this.ticketRepository = ticketRepository;
        this.ticketNotaRepository = ticketNotaRepository;
        this.ativoService = ativoService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (colaboradorRepository.count() > 0) {
            return;
        }

        // 1. Colaboradores e Equipe de TI
        Colaborador mariana = colaboradorRepository.save(new Colaborador(
                "Mariana Costa", "mariana.costa@deskops.local", "Analista Financeiro Sênior", "Financeiro", PerfilUsuario.COLABORADOR
        ));
        Colaborador rafael = colaboradorRepository.save(new Colaborador(
                "Rafael Mendes", "rafael.mendes@deskops.local", "Coordenador Comercial", "Comercial", PerfilUsuario.COLABORADOR
        ));
        Colaborador camila = colaboradorRepository.save(new Colaborador(
                "Camila Rocha", "camila.rocha@deskops.local", "Especialista de RH", "Recursos Humanos", PerfilUsuario.COLABORADOR
        ));
        Colaborador lucasN1 = colaboradorRepository.save(new Colaborador(
                "Lucas Oliveira (Suporte N1)", "lucas.oliveira@deskops.local", "Técnico de Suporte N1", "Suporte TI", PerfilUsuario.TECNICO
        ));
        Colaborador fernandaN2 = colaboradorRepository.save(new Colaborador(
                "Fernanda Lima (Field Service N2)", "fernanda.lima@deskops.local", "Analista de Infraestrutura N2", "Operações TI", PerfilUsuario.TECNICO
        ));
        Colaborador henriqueAdmin = colaboradorRepository.save(new Colaborador(
                "Henrique Silva (Admin TI)", "henrique.silva@deskops.local", "Coordenador de TI & ITAM", "Operações TI", PerfilUsuario.ADMIN
        ));

        // 2. Inventário de Ativos (ITAM)
        Ativo nbFinanceiro = criarAtivoSeed(
                "TI-00142", TipoAtivo.NOTEBOOK, "Dell", "Latitude 5440", "BR-DL5440-9981",
                "Intel Core i7-1355U", "16 GB DDR4", "512 GB NVMe SSD", "Windows 11 Pro 23H2",
                "00:1B:44:11:3A:B7", "192.168.10.45", StatusAtivo.EM_USO, mariana,
                "Entregue para Mariana Costa (Financeiro) com termo de responsabilidade assinado."
        );

        Ativo nbComercial = criarAtivoSeed(
                "TI-00148", TipoAtivo.NOTEBOOK, "Lenovo", "ThinkPad T14 Gen 4", "BR-LNVT14-4412",
                "AMD Ryzen 7 PRO 7840U", "32 GB LPDDR5", "1 TB NVMe SSD", "Windows 11 Pro 23H2",
                "00:1B:44:22:9C:E1", "192.168.10.62", StatusAtivo.EM_MANUTENCAO, rafael,
                "Equipamento recolhido ao laboratório de Field Service para troca de cooler e pasta térmica."
        );

        Ativo desktopRh = criarAtivoSeed(
                "TI-00155", TipoAtivo.DESKTOP, "HP", "ProDesk 400 G9 Mini", "BR-HP400G9-7720",
                "Intel Core i5-12500T", "16 GB DDR4", "256 GB NVMe SSD", "Windows 11 Pro 23H2",
                "00:1B:44:55:1D:09", "192.168.10.88", StatusAtivo.EM_USO, camila,
                "Estação instalada na baia 04 do setor de Recursos Humanos."
        );

        Ativo switchCore = criarAtivoSeed(
                "TI-00201", TipoAtivo.SWITCH, "Cisco", "Catalyst 2960-X 48 GigE PoE", "FOC2314X09A",
                "Dual-Core ARM Cortex-A9", "512 MB DRAM", "128 MB Flash", "Cisco IOS 15.2(7)E",
                "00:25:90:AA:10:01", "192.168.1.2", StatusAtivo.EM_USO, henriqueAdmin,
                "Switch principal do Rack 01 - Sala de Telecomunicações (2º Andar)."
        );

        Ativo impressoraFin = criarAtivoSeed(
                "TI-00215", TipoAtivo.IMPRESSORA, "Brother", "MFC-L6902DW Laser Corporativa", "BR-BRO6902-310",
                "ARM9 800MHz", "1 GB", "N/A", "Brother Embedded Firmware v1.42",
                "30:05:5C:88:12:F0", "192.168.10.210", StatusAtivo.EM_USO, mariana,
                "Alocada no corredor do departamento Financeiro (IP Fixo via DHCP Reservation)."
        );

        criarAtivoSeed(
                "TI-00190", TipoAtivo.NOTEBOOK, "Dell", "Latitude 3440", "BR-DL3440-1055",
                "Intel Core i5-1335U", "16 GB DDR4", "512 GB NVMe SSD", "Ubuntu 24.04 LTS",
                "00:1B:44:77:4F:11", "192.168.10.190", StatusAtivo.DISPONIVEL, null,
                "Máquina de backup revisada e pronta no estoque da TI para novos colaboradores."
        );

        LocalDateTime agora = LocalDateTime.now();

        // 3. Chamados nas 6 colunas da Máquina de Estados
        // Ticket 1: ABERTO (CRITICA)
        Ticket t1 = criarTicketSeed(
                "TKT-202610-0001",
                "Queda de link VLAN Financeiro no Switch do 2º Andar",
                "Estações do setor Financeiro estão sofrendo perda de pacotes intermitente de 40% ao acessar o ERP corporativo.",
                CategoriaTicket.REDE_ACESSO, PrioridadeTicket.CRITICA, StatusTicket.ABERTO,
                mariana, null, switchCore,
                agora.minusMinutes(45), agora.minusMinutes(45).plusHours(2), null
        );
        ticketNotaRepository.save(new TicketNota(
                t1, mariana, "Pessoal, o fechamento contábil está travando por conta das quedas de conexão.", false, agora.minusMinutes(40)
        ));

        // Ticket 2: EM_ATENDIMENTO (ALTA)
        Ticket t2 = criarTicketSeed(
                "TKT-202610-0002",
                "Desligamento repentino por superaquecimento e tela azul (BSOD)",
                "Notebook desliga sozinho durante reuniões por vídeo com planilha aberta. Ventoinha fazendo ruído alto.",
                CategoriaTicket.HARDWARE, PrioridadeTicket.ALTA, StatusTicket.EM_ATENDIMENTO,
                rafael, fernandaN2, nbComercial,
                agora.minusHours(3), agora.minusHours(3).plusHours(8), null
        );
        ticketNotaRepository.save(new TicketNota(
                t2, fernandaN2, "Equipamento coletado para bancada. Temperatura da CPU atingiu 98°C no stress test.", false, agora.minusHours(2)
        ));
        ticketNotaRepository.save(new TicketNota(
                t2, fernandaN2, "[NOTA INTERNA TI] Verificado desgaste no rolamento do fan cooler OEM. Limpeza efetuada e em teste de burn-in.", true, agora.minusHours(1)
        ));

        // Ticket 3: AGUARDANDO_PECA (ALTA)
        Ticket t3 = criarTicketSeed(
                "TKT-202610-0003",
                "Substituição do cilindro de imagem e fusor da impressora departamental",
                "Impressões saindo com faixas verticais escuras e atolamento constante na bandeja 2.",
                CategoriaTicket.HARDWARE, PrioridadeTicket.ALTA, StatusTicket.AGUARDANDO_PECA,
                mariana, lucasN1, impressoraFin,
                agora.minusHours(6), agora.minusHours(6).plusHours(8), null
        );
        ticketNotaRepository.save(new TicketNota(
                t3, lucasN1, "Solicitado kit de manutenção Brother DR-3460 ao fornecedor homologado. Previsão de entrega hoje às 16h.", false, agora.minusHours(4)
        ));

        // Ticket 4: AGUARDANDO_USUARIO (MEDIA)
        Ticket t4 = criarTicketSeed(
                "TKT-202610-0004",
                "Instalação e liberação de certificado digital A3 no token",
                "Necessito assinar lotes de rescisão no eSocial mas o navegador não reconhece a cadeia ICP-Brasil.",
                CategoriaTicket.SOFTWARE, PrioridadeTicket.MEDIA, StatusTicket.AGUARDANDO_USUARIO,
                camila, lucasN1, desktopRh,
                agora.minusHours(10), agora.minusHours(10).plusHours(24), null
        );
        ticketNotaRepository.save(new TicketNota(
                t4, lucasN1, "Camila, atualizei o driver SafeNet remotamente. Pode conectar o token USB e testar a assinatura?", false, agora.minusHours(5)
        ));

        // Ticket 5: RESOLVIDO (MEDIA)
        Ticket t5 = criarTicketSeed(
                "TKT-202610-0005",
                "Falha de vídeo na porta USB-C / Thunderbolt para segundo monitor",
                "Ao conectar o monitor Dell de 24 polegadas na porta USB-C lateral do Latitude 5440, a imagem pisca.",
                CategoriaTicket.HARDWARE, PrioridadeTicket.MEDIA, StatusTicket.RESOLVIDO,
                mariana, fernandaN2, nbFinanceiro,
                agora.minusHours(18), agora.minusHours(18).plusHours(24), agora.minusHours(14)
        );
        ticketNotaRepository.save(new TicketNota(
                t5, fernandaN2, "Atualizada BIOS da versão 1.12 para 1.18 e firmware Thunderbolt Intel. Testado por 1h sem oscilações.", false, agora.minusHours(14)
        ));

        // Ticket 6: FECHADO (BAIXA)
        Ticket t6 = criarTicketSeed(
                "TKT-202610-0006",
                "Configuração de ramal VoIP Softphone e fone USB",
                "Configurar ramal 4210 no MicroSIP e validar áudio do headset corporativo.",
                CategoriaTicket.TELEFONIA, PrioridadeTicket.BAIXA, StatusTicket.FECHADO,
                camila, lucasN1, desktopRh,
                agora.minusHours(36), agora.minusHours(36).plusHours(48), agora.minusHours(34)
        );
        ticketNotaRepository.save(new TicketNota(
                t6, camila, "Ramal funcionando perfeitamente, obrigada pelo atendimento rápido!", false, agora.minusHours(34)
        ));
    }

    private Ativo criarAtivoSeed(
            String tag, TipoAtivo tipo, String fabricante, String modelo, String serie,
            String cpu, String ram, String disco, String so, String mac, String ip,
            StatusAtivo status, Colaborador colab, String obsHistorico
    ) {
        Ativo a = new Ativo();
        a.setTagPatrimonio(tag);
        a.setTipo(tipo);
        a.setFabricante(fabricante);
        a.setModelo(modelo);
        a.setNumeroSerie(serie);
        a.setProcessador(cpu);
        a.setMemoriaRam(ram);
        a.setArmazenamento(disco);
        a.setSistemaOperacional(so);
        a.setEnderecoMac(mac);
        a.setIpLocal(ip);
        a.setStatus(status);
        a.setColaborador(colab);
        Ativo salvo = ativoRepository.save(a);
        ativoService.registrarHistorico(salvo, obsHistorico);
        return salvo;
    }

    private Ticket criarTicketSeed(
            String protocolo, String titulo, String descricao,
            CategoriaTicket categoria, PrioridadeTicket prioridade, StatusTicket status,
            Colaborador solicitante, Colaborador tecnico, Ativo ativo,
            LocalDateTime abertura, LocalDateTime limiteSla, LocalDateTime fechamento
    ) {
        Ticket t = new Ticket();
        t.setProtocolo(protocolo);
        t.setTitulo(titulo);
        t.setDescricao(descricao);
        t.setCategoria(categoria);
        t.setPrioridade(prioridade);
        t.setStatus(status);
        t.setColaborador(solicitante);
        t.setTecnicoAtribuido(tecnico);
        t.setAtivo(ativo);
        t.setDataAbertura(abertura);
        t.setDataLimiteSla(limiteSla);
        t.setDataFechamento(fechamento);
        Ticket salvo = ticketRepository.save(t);
        if (ativo != null) {
            ativoService.registrarHistorico(
                    ativo,
                    String.format("Vinculado ao chamado [%s] (%s - %s): %s", protocolo, categoria, status, titulo)
            );
        }
        return salvo;
    }
}
