package br.com.fiap.mercadoexpress.security.controller;

import br.com.fiap.mercadoexpress.security.auth.UsuarioAutenticado;
import br.com.fiap.mercadoexpress.security.model.Perfil;
import br.com.fiap.mercadoexpress.security.model.Usuario;
import br.com.fiap.mercadoexpress.security.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * /admin/usuarios - quem se cadastrou no Mercado Express.
 *
 * <p>Somente ROLE_ADMIN (protegido pelo /admin/** do SecurityConfig). Lista as
 * contas de TDS_USERS_MERCADO e permite promover um cliente a administrador.
 * A senha nunca e exibida: nem o hash sai do banco para a tela.</p>
 */
@Controller
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("totalAdmins", usuarioService.contarPorPerfil(Perfil.ADMIN));
        model.addAttribute("totalClientes", usuarioService.contarPorPerfil(Perfil.CLIENTE));
        model.addAttribute("idLogado", logado.getId());
        return "admin/usuarios";
    }

    /**
     * POST /admin/usuarios/{id}/promover. O novo perfil vale a partir do proximo
     * login do usuario promovido (a sessao dele, se houver, segue como estava).
     */
    @PostMapping("/{id}/promover")
    public String promover(@PathVariable Long id, RedirectAttributes flash) {
        Usuario promovido = usuarioService.promoverAdmin(id);
        flash.addFlashAttribute("sucesso",
                promovido.getNome() + " agora é administrador. O novo acesso vale a partir do próximo login.");
        return "redirect:/admin/usuarios";
    }
}
