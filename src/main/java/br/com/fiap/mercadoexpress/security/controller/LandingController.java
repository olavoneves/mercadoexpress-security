package br.com.fiap.mercadoexpress.security.controller;

import br.com.fiap.mercadoexpress.security.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * GET / - landing page publica (templates/index.html).
 *
 * <p>Apresenta o mercado, mostra produtos em destaque vindos do banco e leva
 * aos CTAs "Entrar" e "Criar conta". O catalogo completo e a ficha de cada
 * produto ficam atras do login.</p>
 */
@Controller
@RequiredArgsConstructor
public class LandingController {

    private final ProdutoService produtoService;

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("destaques", produtoService.listarDestaques());
        model.addAttribute("setores", produtoService.setoresAtivos());
        model.addAttribute("totalProdutos", produtoService.contarAtivos());
        return "index";
    }
}
