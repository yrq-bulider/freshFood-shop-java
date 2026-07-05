package com.yan.freshfood.framework.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI freshfoodOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("线上生鲜商场 API 文档")
                        .description("接口文档")
                        .version("v1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("satoken",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("satoken")))
                .addSecurityItem(new SecurityRequirement().addList("satoken"));
    }
}