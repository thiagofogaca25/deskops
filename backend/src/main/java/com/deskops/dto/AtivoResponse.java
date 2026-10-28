package com.deskops.dto;

import com.deskops.domain.entity.Ativo;
import com.deskops.domain.enums.StatusAtivo;
import com.deskops.domain.enums.TipoAtivo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Representação detalhada de um Ativo de TI")
public record AtivoResponse(
        @Schema(description = "UUID do ativo")
        UUID id,
        @Schema(description = "Tag de patrimônio", example = "TI-00142")
        String tagPatrimonio,
        @Schema(description = "Tipo do equipamento", example = "NOTEBOOK")
        TipoAtivo tipo,
        @Schema(description = "Fabricante", example = "Dell")
        String fabricante,
        @Schema(description = "Modelo", example = "Latitude 5440")
        String modelo,
        @Schema(description = "Número de série", example = "BR-DL5440-9981")
        String numeroSerie,
        @Schema(description = "Processador", example = "Intel Core i7-1355U")
        String processador,
        @Schema(description = "Memória RAM", example = "16 GB DDR4")
        String memoriaRam,
        @Schema(description = "Armazenamento", example = "512 GB NVMe SSD")
        String armazenamento,
        @Schema(description = "Sistema Operacional", example = "Windows 11 Pro 23H2")
        String sistemaOperacional,
        @Schema(description = "Endereço MAC", example = "00:1B:44:11:3A:B7")
        String enderecoMac,
        @Schema(description = "Endereço IP local", example = "192.168.10.45")
        String ipLocal,
        @Schema(description = "Status atual", example = "EM_USO")
        StatusAtivo status,
        @Schema(description = "Colaborador responsável atual")
        ColaboradorResponse colaborador
) {
    public static AtivoResponse fromEntity(Ativo a) {
        if (a == null) {
            return null;
        }
        return new AtivoResponse(
                a.getId(),
                a.getTagPatrimonio(),
                a.getTipo(),
                a.getFabricante(),
                a.getModelo(),
                a.getNumeroSerie(),
                a.getProcessador(),
                a.getMemoriaRam(),
                a.getArmazenamento(),
                a.getSistemaOperacional(),
                a.getEnderecoMac(),
                a.getIpLocal(),
                a.getStatus(),
                ColaboradorResponse.fromEntity(a.getColaborador())
        );
    }
}
