package com.serjnn.discountservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI discountServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Discount Service API")
                        .description("Microservice responsible for managing product discounts and broadcasting real-time changes via Apache Kafka.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Discount Service Engineering Team")
                                .email("dev@discountservice.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
