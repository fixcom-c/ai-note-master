package com.ainote.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("随手记 AI 行动管家 API")
                        .version("1.0.0")
                        .description("AI 信息整理与任务管理工具 API 文档")
                        .contact(new Contact().name("AI Note Master")));
    }
}