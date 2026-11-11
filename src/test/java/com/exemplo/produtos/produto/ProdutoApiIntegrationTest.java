package com.exemplo.produtos.produto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Testes de integração de ponta a ponta: HTTP → controller → service → JPA → PostgreSQL real
 * (subido em contêiner pelo Testcontainers, com o schema criado pelas migrações do Flyway).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProdutoApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mvc;

    @Autowired
    ProdutoRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    @Test
    void criaProdutoERetorna201ComLocation() throws Exception {
        criar("cam-001", "Camiseta básica", "59.90", 25)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/produtos/" + idDoUnicoProduto())))
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.sku").value("CAM-001")) // normalizado para maiúsculas
                .andExpect(jsonPath("$.nome").value("Camiseta básica"))
                .andExpect(jsonPath("$.preco").value(59.90))
                .andExpect(jsonPath("$.quantidadeEstoque").value(25))
                .andExpect(jsonPath("$.criadoEm").value(notNullValue()))
                .andExpect(jsonPath("$.atualizadoEm").value(notNullValue()));

        Produto salvo = repository.findAll().getFirst();
        assertThat(salvo.getPreco()).isEqualByComparingTo(new BigDecimal("59.90"));
    }

    @Test
    void buscaProdutoPorId() throws Exception {
        criar("CAM-001", "Camiseta básica", "59.90", 25);

        mvc.perform(get("/api/produtos/{id}", idDoUnicoProduto()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("CAM-001"));
    }

    @Test
    void retorna404QuandoProdutoNaoExiste() throws Exception {
        mvc.perform(get("/api/produtos/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Produto com id 999 não encontrado."));
    }

    @Test
    void listaComPaginacaoEOrdenacaoPorNome() throws Exception {
        criar("SKU-C", "Caneca", "30.00", 1);
        criar("SKU-A", "Abajur", "120.00", 2);
        criar("SKU-B", "Boné", "45.00", 3);

        mvc.perform(get("/api/produtos").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].nome").value(contains("Abajur", "Boné")))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(2))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));

        mvc.perform(get("/api/produtos").param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.conteudo[*].nome").value(contains("Caneca")));

        mvc.perform(get("/api/produtos").param("sort", "preco,asc"))
                .andExpect(jsonPath("$.conteudo[*].nome").value(contains("Caneca", "Boné", "Abajur")));
    }

    @Test
    void filtraPorTrechoDoNomeSemDiferenciarMaiusculas() throws Exception {
        criar("SKU-1", "Camiseta básica", "59.90", 1);
        criar("SKU-2", "Camiseta polo", "99.90", 1);
        criar("SKU-3", "Calça jeans", "159.90", 1);

        mvc.perform(get("/api/produtos").param("nome", "CAMISETA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].sku").value(contains("SKU-1", "SKU-2")))
                .andExpect(jsonPath("$.totalElementos").value(2));
    }

    @Test
    void retorna400AoOrdenarPorCampoInexistente() throws Exception {
        mvc.perform(get("/api/produtos").param("sort", "naoExiste"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Não é possível ordenar por 'naoExiste'."));
    }

    @Test
    void atualizaProduto() throws Exception {
        criar("CAM-001", "Camiseta básica", "59.90", 25);
        Long id = idDoUnicoProduto();

        mvc.perform(put("/api/produtos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("CAM-002", "Camiseta premium", "89.90", 10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.sku").value("CAM-002"))
                .andExpect(jsonPath("$.nome").value("Camiseta premium"))
                .andExpect(jsonPath("$.preco").value(89.90))
                .andExpect(jsonPath("$.quantidadeEstoque").value(10));

        Produto atualizado = repository.findById(id).orElseThrow();
        assertThat(atualizado.getNome()).isEqualTo("Camiseta premium");
        assertThat(atualizado.getAtualizadoEm()).isAfter(atualizado.getCriadoEm());
    }

    @Test
    void atualizarMantendoOProprioSkuNaoGeraConflito() throws Exception {
        criar("CAM-001", "Camiseta básica", "59.90", 25);

        mvc.perform(put("/api/produtos/{id}", idDoUnicoProduto())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("CAM-001", "Camiseta básica", "49.90", 25)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(49.90));
    }

    @Test
    void retorna404AoAtualizarProdutoInexistente() throws Exception {
        mvc.perform(put("/api/produtos/{id}", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("CAM-001", "Camiseta", "59.90", 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void excluiProduto() throws Exception {
        criar("CAM-001", "Camiseta básica", "59.90", 25);
        Long id = idDoUnicoProduto();

        mvc.perform(delete("/api/produtos/{id}", id))
                .andExpect(status().isNoContent());

        assertThat(repository.existsById(id)).isFalse();
        mvc.perform(delete("/api/produtos/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void retorna409AoCriarComSkuJaExistente() throws Exception {
        criar("CAM-001", "Camiseta básica", "59.90", 25);

        criar("cam-001", "Outra camiseta", "10.00", 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um produto com o SKU 'CAM-001'."));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void retorna409AoAtualizarParaSkuDeOutroProduto() throws Exception {
        criar("CAM-001", "Camiseta básica", "59.90", 25);
        Long idDoPrimeiro = idDoUnicoProduto();
        criar("CAM-002", "Camiseta polo", "99.90", 5);

        mvc.perform(put("/api/produtos/{id}", idDoPrimeiro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("CAM-002", "Camiseta básica", "59.90", 25)))
                .andExpect(status().isConflict());
    }

    @Test
    void retorna400ComErrosPorCampoQuandoDadosSaoInvalidos() throws Exception {
        mvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku": "x", "nome": " ", "preco": -1, "quantidadeEstoque": -5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros[*].campo")
                        .value(contains("nome", "preco", "quantidadeEstoque", "sku")))
                .andExpect(jsonPath("$.erros[0].mensagem").value("não deve estar em branco"));

        assertThat(repository.count()).isZero();
    }

    @Test
    void retorna400QuandoCamposObrigatoriosFaltam() throws Exception {
        mvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros", hasSize(4)));
    }

    @Test
    void retorna400QuandoJsonEstaMalformado() throws Exception {
        mvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isto não é json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retorna400QuandoIdNaoENumerico() throws Exception {
        mvc.perform(get("/api/produtos/{id}", "abc"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions criar(String sku, String nome, String preco, int quantidadeEstoque) throws Exception {
        return mvc.perform(post("/api/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(sku, nome, preco, quantidadeEstoque)));
    }

    private static String json(String sku, String nome, String preco, int quantidadeEstoque) {
        return """
                {"sku": "%s", "nome": "%s", "descricao": "Descrição de teste", "preco": %s, "quantidadeEstoque": %d}
                """.formatted(sku, nome, preco, quantidadeEstoque);
    }

    private Long idDoUnicoProduto() {
        return repository.findAll().getFirst().getId();
    }
}
