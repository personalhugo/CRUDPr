package com.exemplo.produtos.produto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String sku;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(name = "quantidade_estoque", nullable = false)
    private int quantidadeEstoque;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    /** Controle de concorrência otimista: impede que duas transações simultâneas sobrescrevam uma à outra. */
    @Version
    private long versao;

    protected Produto() {
        // exigido pelo JPA
    }

    public Produto(String sku, String nome, String descricao, BigDecimal preco, int quantidadeEstoque) {
        atualizar(sku, nome, descricao, preco, quantidadeEstoque);
    }

    public void atualizar(String sku, String nome, String descricao, BigDecimal preco, int quantidadeEstoque) {
        this.sku = normalizarSku(sku);
        this.nome = nome.strip();
        this.descricao = (descricao == null || descricao.isBlank()) ? null : descricao.strip();
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
    }

    /** O SKU é sempre gravado sem espaços nas pontas e em maiúsculas, para que a unicidade não dependa de caixa. */
    public static String normalizarSku(String sku) {
        return sku.strip().toUpperCase(Locale.ROOT);
    }

    @PrePersist
    void aoCriar() {
        criadoEm = agora();
        atualizadoEm = criadoEm;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = agora();
    }

    /** Microssegundos são a precisão do timestamptz do PostgreSQL; assim o valor em memória é igual ao gravado. */
    private static Instant agora() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
