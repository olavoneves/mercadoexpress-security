package br.com.fiap.mercadoexpress.security.controller;

import br.com.fiap.mercadoexpress.security.auth.UsuarioAutenticado;
import br.com.fiap.mercadoexpress.security.dto.ContaForm;
import br.com.fiap.mercadoexpress.security.dto.SenhaForm;
import br.com.fiap.mercadoexpress.security.model.Usuario;
import br.com.fiap.mercadoexpress.security.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * /minha-conta - cada usuario ve e edita apenas os proprios dados.
 *
 * <p>O id nunca vem da URL nem do formulario: sai do principal autenticado
 * ({@link AuthenticationPrincipal}). Assim nao ha como um cliente editar a
 * conta de outro trocando um numero na requisicao.</p>
 */
@Controller
@RequestMapping("/minha-conta")
@RequiredArgsConstructor
public class ContaController {

    private final UsuarioService usuarioService;

    /** Grava a autenticacao atualizada na sessao HTTP. */
    private final SecurityContextRepository repositorioDeContexto = new HttpSessionSecurityContextRepository();

    @GetMapping
    public String minhaConta(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {
        Usuario usuario = usuarioService.buscarPorId(logado.getId());
        model.addAttribute("usuario", usuario);
        model.addAttribute("conta", new ContaForm(usuario.getNome(), usuario.getEmail()));
        model.addAttribute("troca", new SenhaForm());
        return "conta/minha-conta";
    }

    /** POST /minha-conta - nome e e-mail. */
    @PostMapping
    public String atualizarDados(@AuthenticationPrincipal UsuarioAutenticado logado,
                                 @Valid @ModelAttribute("conta") ContaForm formulario,
                                 BindingResult erros,
                                 Model model,
                                 RedirectAttributes flash,
                                 HttpServletRequest requisicao,
                                 HttpServletResponse resposta) {

        if (!erros.hasFieldErrors("email") && usuarioService.emailEmUsoPorOutro(formulario.getEmail(), logado.getId())) {
            erros.rejectValue("email", "email.emUso", "Este e-mail já pertence a outra conta");
        }
        if (erros.hasErrors()) {
            model.addAttribute("usuario", usuarioService.buscarPorId(logado.getId()));
            model.addAttribute("troca", new SenhaForm());
            return "conta/minha-conta";
        }

        Usuario atualizado = usuarioService.atualizarDados(logado.getId(), formulario);
        // O e-mail e o username: sem isso o cabecalho mostraria o nome antigo
        // e o principal ficaria apontando para um login que nao existe mais.
        renovarSessao(atualizado, requisicao, resposta);

        flash.addFlashAttribute("sucesso", "Seus dados foram atualizados.");
        return "redirect:/minha-conta";
    }

    /** POST /minha-conta/senha - troca de senha com conferencia da senha atual. */
    @PostMapping("/senha")
    public String trocarSenha(@AuthenticationPrincipal UsuarioAutenticado logado,
                              @Valid @ModelAttribute("troca") SenhaForm formulario,
                              BindingResult erros,
                              Model model,
                              RedirectAttributes flash) {

        if (!erros.hasErrors() && !usuarioService.trocarSenha(logado.getId(), formulario)) {
            erros.rejectValue("senhaAtual", "senha.incorreta", "A senha atual não confere");
        }
        if (erros.hasErrors()) {
            Usuario usuario = usuarioService.buscarPorId(logado.getId());
            model.addAttribute("usuario", usuario);
            model.addAttribute("conta", new ContaForm(usuario.getNome(), usuario.getEmail()));
            formulario.setSenhaAtual(null);
            formulario.setSenha(null);
            formulario.setConfirmacaoSenha(null);
            return "conta/minha-conta";
        }

        flash.addFlashAttribute("sucesso", "Senha alterada. Use a nova senha no próximo login.");
        return "redirect:/minha-conta";
    }

    private void renovarSessao(Usuario usuario, HttpServletRequest requisicao, HttpServletResponse resposta) {
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
        principal.eraseCredentials();

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(contexto);
        repositorioDeContexto.saveContext(contexto, requisicao, resposta);
    }
}
