package com.carloslonghi.bcb.config;

import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {

    // Enums viram schemas proprios (ClientPlanType, MessageStatus...) sem precisar de enumAsRef em cada campo
    static {
        ModelResolver.enumsAsRef = true;
    }

    // Respostas: o Jackson escreve todos os campos, inclusive os nulos, entao no contrato todos sao obrigatorios
    // e os que podem vir nulos declaram "null" no tipo. Sem isso, os tipos gerados no frontend
    // (frontend/openapi.json -> npm run api:types) viriam com todos os campos opcionais
    @Bean
    public OpenApiCustomizer responseFieldsRequired() {
        return openApi -> openApi.getComponents().getSchemas().forEach((name, schema) -> {
            if (name.endsWith("Response") && schema.getProperties() != null) {
                schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
            }
        });
    }

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Desafio Irrah - BCB API")
                        .description(
                                """                                
                                        Passos para começar a usar
                                        
                                        1. Criar um cliente na rota: "POST `/clients`"
                                        
                                        2. Autenticar o cliente na rota: "POST `/auth`" com CPF/CNPJ e senha  
                                           - Copie o token retornado (JWT, válido por 8 horas)  
                                           
                                        3. Autorizar no Swagger
                                           - Clique em "Authorize" (canto inferior direito)  
                                           - Insira o token copiado no input e clique em "Authorize"
                                           
                                        4. Chamar os demais endpoints  
                                           - Agora as operações estarão liberadas.
                                """
                        )
                )
                .tags(List.of(
                        new Tag().name("Autenticação").description("Login com CPF/CNPJ e senha"),
                        new Tag().name("Clientes").description("Operações de CRUD e consulta de saldo"),
                        new Tag().name("Mensagens").description("Operações de envio e consulta de mensagens"),
                        new Tag().name("Conversas").description("Operações de consulta de conversas e mensagens"),
                        new Tag().name("Fila").description("Estatísticas e status da fila de mensagens"),
                        new Tag().name("Eventos").description("Status das mensagens em tempo real (SSE)")
                ));
    }
}
