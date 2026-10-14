package br.com.fiap.mercadoexpress.security.controller;

import br.com.fiap.mercadoexpress.security.auth.RedirecionamentoPorPerfilHandler;
import br.com.fiap.mercadoexpress.security.dto.CadastroForm;
import br.com.fiap.mercadoexpress.security.model.Usuario;
import br.com.fiap.mercadoexpress.security.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Telas de entrada: login proprio, cadastro (Sign Up) e a pagina 403.
 *
 * <p>O POST /login e o POST /logout nao aparecem aqui: quem os processa e o
 * filtro do Spring Security, configurado no
 * {@link br.com.fiap.mercadoexpress.security.config.SecurityConfig}.</p>
 *
 * <pre>
 *   GET  /login     -> formulario de login (templates/auth/login.html)
 *   POST /login     -> Spring Security: ok -> /admin ou /catalogo
 *                                       e-mail sem conta -> GET /cadastro?email=...
 *                                       senha errada -> /login?erro
 *   GET  /cadastro  -> formulario de cadastro (templates/auth/cadastro.html)
 *   POST /cadastro  -> grava em TDS_USERS_MERCADO -> /login com o e-mail preenchido
 * </pre>
 */
@Controller
@RequiredArgsConstructor
public class AutenticacaoController {

    private final UsuarioService usuarioService;

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    /** Quem ja esta autenticado nao precisa do formulario: vai para a sua area. */
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String email,
                        Authentication autenticacao,
                        Model model) {
        if (estaLogado(autenticacao)) {
            return "redirect:" + RedirecionamentoPorPerfilHandler.destinoPara(autenticacao);
        }
        model.addAttribute("emailInformado", email == null || email.isBlank() ? null : email.trim());
        return "auth/login";
    }

    // ------------------------------------------------------------------
    // Cadastro (Sign Up)
    // ------------------------------------------------------------------

    /**
     * Formulario de cadastro. Quando chega do failure handler
     * ({@code origem=login}), o e-mail digitado no login ja vem preenchido e a
     * tela explica por que a pessoa foi parar ali.
     */
    @GetMapping("/cadastro")
    public String cadastro(@RequestParam(required = false) String email,
                           @RequestParam(required = false) String origem,
                           Authentication autenticacao,
                           Model model) {
        if (estaLogado(autenticacao)) {
            return "redirect:" + RedirecionamentoPorPerfilHandler.destinoPara(autenticacao);
        }

        CadastroForm formulario = new CadastroForm();
        formulario.setEmail(email);
        model.addAttribute("cadastro", formulario);
        model.addAttribute("vindoDoLogin", "login".equals(origem) && email != null && !email.isBlank());
        return "auth/cadastro";
    }

    /** Grava a conta como CLIENTE e manda para o login com o e-mail preenchido. */
    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("cadastro") CadastroForm formulario,
                            BindingResult erros,
                            Model model,
                            RedirectAttributes redirecionamento) {
        if (erros.hasErrors()) {
            // Por seguranca, a senha nunca volta preenchida para a tela.
            formulario.setSenha(null);
            formulario.setConfirmacaoSenha(null);
            model.addAttribute("vindoDoLogin", false);
            return "auth/cadastro";
        }

        Usuario novo = usuarioService.cadastrarCliente(formulario);

        redirecionamento.addFlashAttribute("sucesso",
                "Conta criada, " + novo.getNome().split("\\s+")[0] + "! Agora é só entrar com a senha que você escolheu.");
        redirecionamento.addAttribute("email", novo.getEmail());
        return "redirect:/login";
    }

    // ------------------------------------------------------------------
    // 403
    // ------------------------------------------------------------------

    /**
     * Pagina 403: usuario autenticado tentando uma rota que nao e do perfil dele.
     *
     * <p>Aceita qualquer verbo de proposito. O Spring Security chega aqui por
     * <em>forward</em>, preservando o metodo original, entao um POST negado
     * precisa cair nesta mesma pagina em vez de virar um 405.</p>
     */
    @RequestMapping("/acesso-negado")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String acessoNegado() {
        return "error/403";
    }

    private boolean estaLogado(Authentication autenticacao) {
        return autenticacao != null
                && autenticacao.isAuthenticated()
                && !(autenticacao instanceof AnonymousAuthenticationToken);
    }
}
