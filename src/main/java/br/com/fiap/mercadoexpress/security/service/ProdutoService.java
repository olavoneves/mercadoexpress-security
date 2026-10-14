package br.com.fiap.mercadoexpress.security.service;

import br.com.fiap.mercadoexpress.security.exception.ProdutoNaoEncontradoException;
import br.com.fiap.mercadoexpress.security.model.Produto;
import br.com.fiap.mercadoexpress.security.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Regras de negocio dos produtos do Mercado Express.
 *
 * <p>Concentra a normalizacao dos filtros e a atualizacao campo a campo, para
 * que os controllers cuidem apenas do HTTP e da montagem do Model.</p>
 */
@Service
@RequiredArgsConstructor
public class ProdutoService {

    /** Setores oferecidos no formulario de cadastro. */
    public static final List<String> SETORES = List.of(
            "Hortifruti",
            "Padaria",
            "Açougue",
            "Frios e Laticínios",
            "Mercearia",
            "Bebidas",
            "Higiene e Limpeza",
            "Bazar");

    private final ProdutoRepository repository;

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /** Catalogo: so produtos ativos, com busca por nome e filtro de setor. */
    @Transactional(readOnly = true)
    public Page<Produto> listarCatalogo(String busca, String setor, Pageable paginacao) {
        return repository.buscarNoCatalogo(Boolean.TRUE, normalizar(busca), normalizar(setor), paginacao);
    }

    /** Painel administrativo: ativos e inativos, paginados. */
    @Transactional(readOnly = true)
    public Page<Produto> listarPainel(String busca, String setor, Pageable paginacao) {
        return repository.buscarNoPainel(normalizar(busca), normalizar(setor), paginacao);
    }

    /** Landing page: ate oito produtos ativos (duas fileiras de quatro) com estoque, os mais recentes primeiro. */
    @Transactional(readOnly = true)
    public List<Produto> listarDestaques() {
        return repository.findTop8ByAtivoTrueAndEstoqueGreaterThanOrderByDataCadastroDescIdDesc(0);
    }

    @Transactional(readOnly = true)
    public Produto buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    /** Setores que realmente estao no catalogo, para o filtro e a landing. */
    @Transactional(readOnly = true)
    public List<String> setoresAtivos() {
        return repository.listarSetoresAtivos();
    }

    @Transactional(readOnly = true)
    public long contarAtivos() {
        return repository.contarPorSituacao(Boolean.TRUE);
    }

    @Transactional(readOnly = true)
    public long contarInativos() {
        return repository.contarPorSituacao(Boolean.FALSE);
    }

    // ------------------------------------------------------------------
    // Comandos
    // ------------------------------------------------------------------

    @Transactional
    public Produto criar(Produto produto) {
        produto.setId(null);
        if (produto.getDataCadastro() == null) {
            produto.setDataCadastro(LocalDate.now());
        }
        if (produto.getAtivo() == null) {
            produto.setAtivo(Boolean.FALSE);
        }
        return repository.save(produto);
    }

    /**
     * Atualiza o produto existente campo a campo. O id e a data de cadastro
     * originais sao preservados, mesmo que o formulario tente sobrescreve-los.
     */
    @Transactional
    public Produto atualizar(Long id, Produto formulario) {
        Produto existente = buscarPorId(id);
        existente.setNome(formulario.getNome());
        existente.setTipo(formulario.getTipo());
        existente.setSetor(formulario.getSetor());
        existente.setTamanho(formulario.getTamanho());
        existente.setPreco(formulario.getPreco());
        existente.setDescricao(formulario.getDescricao());
        existente.setEstoque(formulario.getEstoque());
        existente.setAtivo(formulario.getAtivo() != null ? formulario.getAtivo() : Boolean.FALSE);
        return repository.save(existente);
    }

    /** Exclusao definitiva, acionada pelo botao "Excluir" do painel. */
    @Transactional
    public String excluir(Long id) {
        Produto existente = buscarPorId(id);
        repository.delete(existente);
        return existente.getNome();
    }

    /**
     * Filtro ausente vira texto vazio (= condicao desligada na consulta). Nunca
     * null: ver o comentario do {@link ProdutoRepository} sobre o PostgreSQL.
     */
    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
