package com.hbs.entitysense.config;

import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI entitySenseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EntitySense API")
                        .description("Detect sanctioned or mule entities during fund transfers")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EntitySense Team")
                                .email("support@entitysense.ai")));
    }
}
