package com.exemplo.produtos.produto.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.exemplo.produtos.produto.Produto;

/** Representação de um produto devolvida pela API. */
public record ProdutoResponse(
        Long id,
        String sku,
        String nome,
        String descricao,
        BigDecimal preco,
        int quantidadeEstoque,
        Instant criadoEm,
        Instant atualizadoEm) {

    public static ProdutoResponse de(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getSku(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.getQuantidadeEstoque(),
                produto.getCriadoEm(),
                produto.getAtualizadoEm());
    }
}
