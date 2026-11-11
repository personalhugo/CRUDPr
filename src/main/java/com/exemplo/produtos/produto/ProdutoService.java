package com.exemplo.produtos.produto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exemplo.produtos.comum.erro.ConflitoException;
import com.exemplo.produtos.comum.erro.RecursoNaoEncontradoException;
import com.exemplo.produtos.produto.dto.ProdutoRequest;
import com.exemplo.produtos.produto.dto.ProdutoResponse;

@Service
@Transactional(readOnly = true)
public class ProdutoService {

    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        String sku = Produto.normalizarSku(request.sku());
        if (repository.existsBySku(sku)) {
            throw skuEmUso(sku);
        }

        Produto produto = new Produto(sku, request.nome(), request.descricao(),
                request.preco(), request.quantidadeEstoque());
        return ProdutoResponse.de(repository.saveAndFlush(produto));
    }

    public ProdutoResponse buscarPorId(Long id) {
        return ProdutoResponse.de(obter(id));
    }

    /** Lista paginada; se {@code nome} for informado, filtra pelos produtos cujo nome contém o trecho. */
    public Page<ProdutoResponse> listar(String nome, Pageable pageable) {
        Page<Produto> pagina = (nome == null || nome.isBlank())
                ? repository.findAll(pageable)
                : repository.findByNomeContainingIgnoreCase(nome.strip(), pageable);
        return pagina.map(ProdutoResponse::de);
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = obter(id);

        String sku = Produto.normalizarSku(request.sku());
        if (repository.existsBySkuAndIdNot(sku, id)) {
            throw skuEmUso(sku);
        }

        produto.atualizar(sku, request.nome(), request.descricao(),
                request.preco(), request.quantidadeEstoque());
        // O flush explícito garante que "atualizadoEm" já esteja preenchido na resposta.
        return ProdutoResponse.de(repository.saveAndFlush(produto));
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(obter(id));
    }

    private Produto obter(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Produto com id %d não encontrado.".formatted(id)));
    }

    private static ConflitoException skuEmUso(String sku) {
        return new ConflitoException("Já existe um produto com o SKU '%s'.".formatted(sku));
    }
}
