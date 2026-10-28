package com.deskops.dto;

import com.deskops.domain.enums.StatusAtivo;
import com.deskops.domain.enums.TipoAtivo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

@Schema(description = "Dados de entrada para cadastro ou atualização de um Ativo de TI")
public record AtivoRequest(
        @Schema(description = "Tag de patrimônio única no formato TI-XXXXX", example = "TI-00142")
        @NotBlank(message = "A tag de patrimônio é obrigatória")
        @Pattern(regexp = "^TI-\\d{4,6}$", message = "A tag de patrimônio deve seguir o padrão TI-XXXXX (ex: TI-00142)")
        String tagPatrimonio,

        @Schema(description = "Categoria do ativo", example = "NOTEBOOK")
        @NotNull(message = "O tipo do ativo é obrigatório")
        TipoAtivo tipo,

        @Schema(description = "Fabricante do equipamento", example = "Dell")
        @NotBlank(message = "O fabricante é obrigatório")
        String fabricante,

        @Schema(description = "Modelo do equipamento", example = "Latitude 5440")
        @NotBlank(message = "O modelo é obrigatório")
        String modelo,

        @Schema(description = "Número de série (Service Tag / Serial)", example = "BR-DL5440-9981")
        String numeroSerie,

        @Schema(description = "Especificação de processador", example = "Intel Core i7-1355U")
        String processador,

        @Schema(description = "Capacidade de memória RAM", example = "16 GB DDR4")
        String memoriaRam,

        @Schema(description = "Armazenamento interno", example = "512 GB NVMe SSD")
        String armazenamento,

        @Schema(description = "Sistema Operacional instalado", example = "Windows 11 Pro 23H2")
        String sistemaOperacional,

        @Schema(description = "Endereço físico MAC", example = "00:1B:44:11:3A:B7")
        String enderecoMac,

        @Schema(description = "Endereço IP local na rede corporativa", example = "192.168.10.45")
        String ipLocal,

        @Schema(description = "Status operacional do ativo", example = "EM_USO")
        StatusAtivo status,

        @Schema(description = "ID do colaborador responsável pelo ativo (opcional)")
        UUID colaboradorId
) {
}
