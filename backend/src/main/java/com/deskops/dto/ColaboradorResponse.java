package com.deskops.dto;

import com.deskops.domain.entity.Colaborador;
import com.deskops.domain.enums.PerfilUsuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Representação de um Colaborador cadastrado")
public record ColaboradorResponse(
        @Schema(description = "Identificador único do colaborador", example = "b4b12345-7890-4abc-def0-1234567890ab")
        UUID id,

        @Schema(description = "Nome completo", example = "Mariana Costa")
        String nome,

        @Schema(description = "E-mail corporativo", example = "mariana.costa@deskops.local")
        String email,

        @Schema(description = "Cargo", example = "Analista Financeiro Sênior")
        String cargo,

        @Schema(description = "Departamento", example = "Financeiro")
        String departamento,

        @Schema(description = "Perfil de acesso", example = "COLABORADOR")
        PerfilUsuario perfil
) {
    public static ColaboradorResponse fromEntity(Colaborador c) {
        if (c == null) {
            return null;
        }
        return new ColaboradorResponse(
                c.getId(),
                c.getNome(),
                c.getEmail(),
                c.getCargo(),
                c.getDepartamento(),
                c.getPerfil()
        );
    }
}
