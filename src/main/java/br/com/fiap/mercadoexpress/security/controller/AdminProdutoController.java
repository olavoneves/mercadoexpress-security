package br.com.fiap.mercadoexpress.security.controller;

import br.com.fiap.mercadoexpress.security.model.Produto;
import br.com.fiap.mercadoexpress.security.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * Area do administrador: o CRUD completo de produtos.
 *
 * <p>Todo o mapeamento nasce em /admin, que o SecurityConfig reserva para
 * ROLE_ADMIN. Cada operacao que altera dados termina em um redirect com
 * mensagem flash, evitando reenvio de formulario no F5.</p>
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminProdutoController {

    private final ProdutoService service;

    /** Alimenta o select de setor em todos os formularios desta area. */
    @ModelAttribute("setoresDisponiveis")
    public List<String> setoresDisponiveis() {
        return ProdutoService.SETORES;
    }

    // ------------------------------------------------------------------
    // READ - painel
    // ------------------------------------------------------------------

    /** GET /admin - tabela densa com todos os produtos, paginada. */
    @GetMapping
    public String painel(@RequestParam(required = false) String busca,
                         @RequestParam(required = false) String setor,
                         @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable paginacao,
                         Model model) {

        Page<Produto> pagina = service.listarPainel(busca, setor, paginacao);

        model.addAttribute("pagina", pagina);
        model.addAttribute("busca", busca);
        model.addAttribute("setorSelecionado", setor);
        model.addAttribute("totalAtivos", service.contarAtivos());
        model.addAttribute("totalInativos", service.contarInativos());

        return "admin/painel";
    }

    // ------------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------------

    /** GET /admin/produtos/novo - formulario em branco. */
    @GetMapping("/produtos/novo")
    public String formularioDeCadastro(Model model) {
        Produto novo = new Produto();
        novo.setAtivo(Boolean.TRUE);
        novo.setEstoque(0);
        novo.setDataCadastro(LocalDate.now());

        model.addAttribute("produto", novo);
        model.addAttribute("edicao", false);
        return "admin/formulario";
    }

    /** POST /admin/produtos - grava o novo produto. */
    @PostMapping("/produtos")
    public String criar(@Valid @ModelAttribute("produto") Produto produto,
                        BindingResult erros,
                        Model model,
                        RedirectAttributes flash) {

        if (erros.hasErrors()) {
            model.addAttribute("edicao", false);
            return "admin/formulario";
        }

        Produto salvo = service.criar(produto);
        flash.addFlashAttribute("sucesso", "Produto \"" + salvo.getNome() + "\" cadastrado com sucesso.");
        return "redirect:/admin";
    }

    // ------------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------------

    /** GET /admin/produtos/{id}/editar - formulario preenchido. */
    @GetMapping("/produtos/{id}/editar")
    public String formularioDeEdicao(@PathVariable Long id, Model model) {
        model.addAttribute("produto", service.buscarPorId(id));
        model.addAttribute("edicao", true);
        return "admin/formulario";
    }

    /** POST /admin/produtos/{id} - aplica a edicao. */
    @PostMapping("/produtos/{id}")
    public String atualizar(@PathVariable Long id,
                            @Valid @ModelAttribute("produto") Produto produto,
                            BindingResult erros,
                            Model model,
                            RedirectAttributes flash) {

        if (erros.hasErrors()) {
            model.addAttribute("edicao", true);
            model.addAttribute("idEmEdicao", id);
            return "admin/formulario";
        }

        Produto salvo = service.atualizar(id, produto);
        flash.addFlashAttribute("sucesso", "Produto \"" + salvo.getNome() + "\" atualizado com sucesso.");
        return "redirect:/admin";
    }

    // ------------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------------

    /**
     * GET /admin/produtos/{id}/excluir - tela de confirmacao. Funciona mesmo com
     * JavaScript desligado e deixa claro o que sera apagado.
     */
    @GetMapping("/produtos/{id}/excluir")
    public String confirmarExclusao(@PathVariable Long id, Model model) {
        model.addAttribute("produto", service.buscarPorId(id));
        return "admin/excluir";
    }

    /**
     * POST /admin/produtos/{id}/excluir - remove o produto. E POST (nunca GET)
     * para que nenhum link ou robo consiga apagar dados, e leva o token CSRF.
     */
    @PostMapping("/produtos/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes flash) {
        String nome = service.excluir(id);
        flash.addFlashAttribute("sucesso", "Produto \"" + nome + "\" excluído definitivamente.");
        return "redirect:/admin";
    }
}
