package com.deskops;

import com.deskops.domain.enums.*;
import com.deskops.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeskOpsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Fluxo completo E2E: Colaboradores, Ativos, Chamados, SLA, Máquina de Estados, Notas, Diagnóstico e RFC 7807")
    void deveExecutarFluxoCompletoComValidacoesRfc7807() throws Exception {
        // 1. Cadastrar Colaborador Solicitante e Técnico de Suporte
        ColaboradorRequest solicitanteReq = new ColaboradorRequest(
                "Ana Beatriz Souza",
                "ana.souza@deskops.local",
                "Analista Contábil",
                "Contabilidade",
                PerfilUsuario.COLABORADOR
        );

        MvcResult solicitanteResult = mockMvc.perform(post("/api/colaboradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitanteReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("ana.souza@deskops.local"))
                .andReturn();

        UUID solicitanteId = UUID.fromString(
                objectMapper.readTree(solicitanteResult.getResponse().getContentAsString()).get("id").asText()
        );

        // Testar duplicidade de e-mail -> 409 Conflict (RFC 7807 ProblemDetail)
        mockMvc.perform(post("/api/colaboradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitanteReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de Dados"))
                .andExpect(jsonPath("$.status").value(409));

        ColaboradorRequest tecnicoReq = new ColaboradorRequest(
                "Diego Martins (Suporte N2)",
                "diego.martins@deskops.local",
                "Analista de Suporte N2",
                "Suporte TI",
                PerfilUsuario.TECNICO
        );

        MvcResult tecnicoResult = mockMvc.perform(post("/api/colaboradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tecnicoReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID tecnicoId = UUID.fromString(
                objectMapper.readTree(tecnicoResult.getResponse().getContentAsString()).get("id").asText()
        );

        // 2. Cadastrar Ativo de TI e validar regras de domínio
        AtivoRequest ativoInvalidoReq = new AtivoRequest(
                "TAG-INVALIDA",
                TipoAtivo.NOTEBOOK,
                "Dell",
                "Latitude 5440",
                "SN-123",
                "Core i7",
                "16GB",
                "512GB SSD",
                "Windows 11",
                "00:11:22:33:44:55",
                "192.168.10.99",
                StatusAtivo.EM_USO,
                solicitanteId
        );

        // Formato de tag inválido -> 400 Bad Request com ProblemDetail
        mockMvc.perform(post("/api/ativos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ativoInvalidoReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Falha de Validação de Entrada"))
                .andExpect(jsonPath("$.errors.tagPatrimonio").exists());

        // Ativo DESCARTADO não pode ter colaborador -> 422 Unprocessable Entity
        AtivoRequest ativoDescartadoComColab = new AtivoRequest(
                "TI-00999",
                TipoAtivo.DESKTOP,
                "HP",
                "Elitedesk",
                "SN-999",
                "Core i5",
                "8GB",
                "256GB",
                "Windows 10",
                "00:11:22:33:44:99",
                "192.168.10.199",
                StatusAtivo.DESCARTADO,
                solicitanteId
        );

        mockMvc.perform(post("/api/ativos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ativoDescartadoComColab)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        // Cadastrar ativo válido
        AtivoRequest ativoValidoReq = new AtivoRequest(
                "TI-00501",
                TipoAtivo.NOTEBOOK,
                "Dell",
                "Latitude 5440",
                "BR-DL5440-501",
                "Intel Core i7-1355U",
                "16 GB DDR4",
                "512 GB NVMe SSD",
                "Windows 11 Pro",
                "AA:BB:CC:DD:EE:01",
                "192.168.10.105",
                StatusAtivo.EM_USO,
                solicitanteId
        );

        MvcResult ativoResult = mockMvc.perform(post("/api/ativos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ativoValidoReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tagPatrimonio").value("TI-00501"))
                .andExpect(jsonPath("$.status").value("EM_USO"))
                .andExpect(jsonPath("$.colaborador.nome").value("Ana Beatriz Souza"))
                .andReturn();

        UUID ativoId = UUID.fromString(
                objectMapper.readTree(ativoResult.getResponse().getContentAsString()).get("id").asText()
        );

        // 3. Abrir Chamado vinculado ao Ativo com prioridade CRITICA (SLA +2h)
        TicketRequest ticketReq = new TicketRequest(
                "SSD apresentando falha de leitura S.M.A.R.T.",
                "Sistema operacional exibe alerta de falha iminente de disco ao iniciar.",
                CategoriaTicket.HARDWARE,
                PrioridadeTicket.CRITICA,
                solicitanteId,
                ativoId,
                null
        );

        MvcResult ticketResult = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ticketReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocolo", startsWith("TKT-")))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.prioridade").value("CRITICA"))
                .andExpect(jsonPath("$.horasSlaPadrao").value(2))
                .andReturn();

        JsonNode ticketNode = objectMapper.readTree(ticketResult.getResponse().getContentAsString());
        UUID ticketId = UUID.fromString(ticketNode.get("id").asText());

        // 4. Tentar transicionar para EM_ATENDIMENTO sem técnico -> 422 Unprocessable Entity
        TicketStatusUpdateRequest semTecnicoReq = new TicketStatusUpdateRequest(
                StatusTicket.EM_ATENDIMENTO,
                null,
                "Tentativa sem técnico",
                false
        );

        mockMvc.perform(patch("/api/tickets/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(semTecnicoReq)))
                .andExpect(status().isUnprocessableEntity());

        // 5. Transicionar ABERTO -> EM_ATENDIMENTO com técnico atribuído
        TicketStatusUpdateRequest assumirReq = new TicketStatusUpdateRequest(
                StatusTicket.EM_ATENDIMENTO,
                tecnicoId,
                "Chamado assumido pela equipe de Field Service N2.",
                false
        );

        mockMvc.perform(patch("/api/tickets/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assumirReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.tecnicoAtribuido.id").value(tecnicoId.toString()));

        // 6. Transicionar EM_ATENDIMENTO -> AGUARDANDO_PECA (deve colocar o ativo em EM_MANUTENCAO automaticamente)
        TicketStatusUpdateRequest aguardandoPecaReq = new TicketStatusUpdateRequest(
                StatusTicket.AGUARDANDO_PECA,
                tecnicoId,
                "Aguardando liberação de SSD NVMe 512GB no almoxarifado.",
                true
        );

        mockMvc.perform(patch("/api/tickets/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aguardandoPecaReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PECA"))
                .andExpect(jsonPath("$.ativo.status").value("EM_MANUTENCAO"));

        // 7. Adicionar nota pública e nota privada de TI na Timeline
        TicketNotaRequest notaPublica = new TicketNotaRequest(
                tecnicoId,
                "Novo SSD instalado e imagem corporativa restaurada.",
                false
        );
        TicketNotaRequest notaPrivada = new TicketNotaRequest(
                tecnicoId,
                "[TI INTERNO] Serial do SSD antigo: NVME-OLD-991. Acionar garantia OEM Dell.",
                true
        );

        mockMvc.perform(post("/api/tickets/{id}/notas", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notaPublica)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tickets/{id}/notas", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notaPrivada)))
                .andExpect(status().isCreated());

        // Verificar filtro de notas públicas vs todas as notas
        mockMvc.perform(get("/api/tickets/{id}/notas", ticketId).param("incluirPrivadas", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4))); // 2 de transição + 2 manuais

        mockMvc.perform(get("/api/tickets/{id}/notas", ticketId).param("incluirPrivadas", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))); // apenas as 2 públicas

        // 8. Transicionar AGUARDANDO_PECA -> RESOLVIDO -> FECHADO (deve restaurar ativo para EM_USO)
        TicketStatusUpdateRequest resolverReq = new TicketStatusUpdateRequest(
                StatusTicket.RESOLVIDO,
                tecnicoId,
                "Equipamento entregue e validado.",
                false
        );

        mockMvc.perform(patch("/api/tickets/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolverReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVIDO"))
                .andExpect(jsonPath("$.dataFechamento").isNotEmpty())
                .andExpect(jsonPath("$.ativo.status").value("EM_USO"));

        TicketStatusUpdateRequest fecharReq = new TicketStatusUpdateRequest(
                StatusTicket.FECHADO,
                tecnicoId,
                "Encerramento confirmado pelo usuário.",
                false
        );

        mockMvc.perform(patch("/api/tickets/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fecharReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FECHADO"));

        // Chamado FECHADO é imutável -> 422 ao tentar adicionar nota
        mockMvc.perform(post("/api/tickets/{id}/notas", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notaPublica)))
                .andExpect(status().isUnprocessableEntity());

        // 9. Consultar Diagnóstico do Ativo (hardware + chamados vinculados + histórico)
        MvcResult diagResult = mockMvc.perform(get("/api/ativos/{id}/diagnostico", ativoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo.tagPatrimonio").value("TI-00501"))
                .andExpect(jsonPath("$.totalIncidenteHardware").value(1))
                .andExpect(jsonPath("$.chamadosVinculados", hasSize(1)))
                .andExpect(jsonPath("$.historicoPatrimonial", hasSize(greaterThanOrEqualTo(4))))
                .andReturn();

        assertThat(diagResult.getResponse().getContentAsString()).contains("TKT-");

        // 10. Consultar Métricas do Dashboard, Actuator Health e OpenAPI Docs
        mockMvc.perform(get("/api/dashboard/metricas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalAtivos").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());
    }
}
