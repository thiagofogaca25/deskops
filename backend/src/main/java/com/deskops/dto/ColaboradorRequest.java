package com.deskops.dto;

import com.deskops.domain.enums.PerfilUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados de entrada para cadastro ou atualização de um Colaborador")
public record ColaboradorRequest(
        @Schema(description = "Nome completo do colaborador", example = "Mariana Costa")
        @NotBlank(message = "O nome do colaborador é obrigatório")
        String nome,

        @Schema(description = "E-mail corporativo único", example = "mariana.costa@deskops.local")
        @NotBlank(message = "O e-mail do colaborador é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Schema(description = "Cargo na empresa", example = "Analista Financeiro Sênior")
        @NotBlank(message = "O cargo é obrigatório")
        String cargo,

        @Schema(description = "Departamento ou setor", example = "Financeiro")
        @NotBlank(message = "O departamento é obrigatório")
        String departamento,

        @Schema(description = "Perfil de acesso no sistema", example = "COLABORADOR")
        PerfilUsuario perfil
) {
}
