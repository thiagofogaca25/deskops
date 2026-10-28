package com.deskops.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI deskOpsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DeskOps API — Help Desk & IT Asset Management (ITAM)")
                        .description("API REST para gerenciamento integrado de Chamados de Suporte Técnico (N1/N2/Field Service), " +
                                "Inventário Patrimonial de Hardware, Controle de SLA e Diagnóstico de Ativos.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe de Operações e Infraestrutura de TI — DeskOps")
                                .email("suporte.ti@deskops.local"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
