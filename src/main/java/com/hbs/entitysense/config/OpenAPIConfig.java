package com.hbs.entitysense.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.hbs.entitysense.constants.EntitySenseConstant.*;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI entitySenseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(OPEN_API_SPEC_TITLE)
                        .description(OPEN_API_SPEC_DESCRIPTION)
                        .version(OPEN_API_SPEC_VERSION));
    }
}
