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
}
