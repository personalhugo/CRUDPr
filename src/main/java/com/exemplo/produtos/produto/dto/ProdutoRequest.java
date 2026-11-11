package com.exemplo.produtos.produto.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Dados recebidos na criação (POST) e na atualização (PUT) de um produto. */
public record ProdutoRequest(

        @Schema(description = "Código único do produto", example = "CAM-001")
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9-]{3,40}$",
                message = "deve ter de 3 a 40 caracteres, apenas letras, números e hífen")
        String sku,

        @Schema(example = "Camiseta básica")
        @NotBlank
        @Size(max = 120)
        String nome,

        @Schema(example = "Camiseta 100% algodão, cor preta")
        @Size(max = 1000)
        String descricao,

        @Schema(example = "59.90")
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal preco,

        @Schema(example = "25")
        @NotNull
        @PositiveOrZero
        Integer quantidadeEstoque) {
}
