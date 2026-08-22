package com.ledgerflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "ApiKeyAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("LedgerFlow — Production-Grade Payment & Order Infrastructure API")
                        .version("1.0.0")
                        .description("High-reliability fintech and e-commerce payment infrastructure engine featuring " +
                                "atomic multi-aggregate transactions, double-entry financial ledger accounting, " +
                                "first-class idempotency with replay safety, transactional outbox messaging, " +
                                "RabbitMQ DLQ routing, and Redis rate limiting.")
                        .contact(new Contact().name("LedgerFlow Architecture Team").email("engineering@ledgerflow.io"))
                        .license(new License().name("Apache 2.0").url("https://spring.io")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name("X-API-KEY")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("API Key for administrative endpoints")));
    }
}
