package com.exemplo.produtos.comum;

import java.util.List;

import org.springframework.data.domain.Page;

/** Formato estável de resposta paginada, independente da estrutura interna do Spring Data. */
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas) {

    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
