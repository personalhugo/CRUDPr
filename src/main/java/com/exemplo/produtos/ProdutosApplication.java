package com.exemplo.produtos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "API de Produtos", version = "1.0.0",
        description = "CRUD de produtos com Spring Boot e PostgreSQL"))
public class ProdutosApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProdutosApplication.class, args);
    }
}
