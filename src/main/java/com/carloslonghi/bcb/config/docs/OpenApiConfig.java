package com.carloslonghi.bcb.config.docs;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "UUID"
)
public class OpenApiConfig {
    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Desafio Backend - BCB API")
                        .description(
                                """                                
                                        Passos para começar a usar
                                        
                                        1. Criar um cliente na rota: "POST `/clients`"
                                        
                                        2. Autenticar o cliente na rota: "POST `/auth`" usando CPF/CNPJ  
                                           - Copie o token retornado (UUID)  
                                           
                                        3. Autorizar no Swagger
                                           - Clique em "Authorize" (canto inferior direito)  
                                           - Insira o token copiado no input e clique em "Authorize"
                                           
                                        4. Chamar os demais endpoints  
                                           - Agora as operações estarão liberadas.
                                """
                        )
                )
                .tags(List.of(
                        new Tag().name("Autenticação").description("Operações de login via CPF/CNPJ"),
                        new Tag().name("Clientes").description("Operações de CRUD e consulta de saldo"),
                        new Tag().name("Mensagens").description("Operações de envio e consulta de mensagens"),
                        new Tag().name("Conversas").description("Operações de consulta de conversas e mensagens"),
                        new Tag().name("Fila").description("Estatísticas e status da fila de mensagens")
                ));
    }
}
