package com.dcim.platform.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("机房动环监控平台 API")
                        .version("1.0")
                        .description("基于中国铁塔B接口2016协议的动环监控平台后端接口")
                        .contact(new Contact().name("DCIM Team")));
    }
}
