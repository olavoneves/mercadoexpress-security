package br.com.fiap.mercadoexpress.security.controller;

import br.com.fiap.mercadoexpress.security.auth.UsuarioAutenticado;
import br.com.fiap.mercadoexpress.security.exception.ProdutoNaoEncontradoException;
import br.com.fiap.mercadoexpress.security.model.Produto;
import br.com.fiap.mercadoexpress.security.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Area do cliente: catalogo e ficha do produto.
 *
 * <p>As duas rotas exigem login com ROLE_CLIENTE ou ROLE_ADMIN. E a tela para
 * onde o cliente e levado logo depois de entrar.</p>
 */
@Controller
@RequiredArgsConstructor
public class CatalogoController {

    private final ProdutoService service;

    /**
     * GET /catalogo - grade de produtos ativos, com busca por nome e filtro por
     * setor chegando como parametros de consulta.
     */
    @GetMapping("/catalogo")
    public String catalogo(@RequestParam(required = false) String busca,
                           @RequestParam(required = false) String setor,
                           @PageableDefault(size = 12, sort = "nome", direction = Sort.Direction.ASC) Pageable paginacao,
                           Model model) {

        Page<Produto> pagina = service.listarCatalogo(busca, setor, paginacao);

        model.addAttribute("pagina", pagina);
        model.addAttribute("setores", service.setoresAtivos());
        model.addAttribute("busca", busca);
        model.addAttribute("setorSelecionado", setor);
        model.addAttribute("temFiltro", (busca != null && !busca.isBlank()) || (setor != null && !setor.isBlank()));

        return "catalogo/index";
    }

    /**
     * GET /produtos/{id} - ficha completa. Produto fora de linha so e visivel
     * para o administrador; para o cliente ele simplesmente nao existe.
     */
    @GetMapping("/produtos/{id}")
    public String detalhe(@PathVariable Long id,
                          @AuthenticationPrincipal UsuarioAutenticado usuario,
                          Model model) {
        Produto produto = service.buscarPorId(id);
        if (!Boolean.TRUE.equals(produto.getAtivo()) && (usuario == null || !usuario.isAdmin())) {
            throw new ProdutoNaoEncontradoException(id);
        }
        model.addAttribute("produto", produto);
        return "catalogo/detalhe";
    }
}
