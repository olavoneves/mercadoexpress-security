package br.com.fiap.mercadoexpress.security.repository;

import br.com.fiap.mercadoexpress.security.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA da entidade {@link Produto}.
 *
 * <p>Alem do CRUD herdado de {@link JpaRepository}, declara as consultas de
 * busca usadas pelo catalogo e pelo painel administrativo. Os filtros sao
 * opcionais: quando o parametro chega nulo, aquela condicao e ignorada.</p>
 */
@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    /**
     * Catalogo: apenas produtos na situacao informada (ativos), opcionalmente
     * filtrados por trecho do nome e por setor.
     */
    @Query("""
            select p from Produto p
            where p.ativo = :ativo
              and (:busca is null or upper(p.nome) like upper(concat('%', :busca, '%')))
              and (:setor is null or p.setor = :setor)
            """)
    Page<Produto> buscarNoCatalogo(@Param("ativo") Boolean ativo,
                                   @Param("busca") String busca,
                                   @Param("setor") String setor,
                                   Pageable paginacao);

    /** Painel administrativo: enxerga ativos e inativos, filtrando por nome e setor. */
    @Query("""
            select p from Produto p
            where (:busca is null or upper(p.nome) like upper(concat('%', :busca, '%')))
              and (:setor is null or p.setor = :setor)
            """)
    Page<Produto> buscarNoPainel(@Param("busca") String busca,
                                 @Param("setor") String setor,
                                 Pageable paginacao);

    /** Destaques da landing page: os ativos com estoque mais recentes. */
    List<Produto> findTop6ByAtivoTrueAndEstoqueGreaterThanOrderByDataCadastroDescIdDesc(Integer estoqueMinimo);

    /** Alimenta o filtro de setor com o que realmente esta no catalogo. */
    @Query("select distinct p.setor from Produto p where p.ativo = true order by p.setor")
    List<String> listarSetoresAtivos();

    /** Contador do painel, exibido ao lado do titulo. */
    @Query("select count(p) from Produto p where p.ativo = :ativo")
    long contarPorSituacao(@Param("ativo") Boolean ativo);
}
