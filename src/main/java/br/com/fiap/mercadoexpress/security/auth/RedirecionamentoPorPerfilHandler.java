package br.com.fiap.mercadoexpress.security.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Depois do login correto, cada perfil cai direto na sua tela restrita:
 *
 * <ul>
 *   <li>ROLE_ADMIN &rarr; /admin (painel com o CRUD de produtos);</li>
 *   <li>ROLE_CLIENTE &rarr; /catalogo.</li>
 * </ul>
 *
 * <p>O destino e sempre o do perfil, mesmo que a pessoa tenha tentado outra
 * rota antes do login: assim o cliente nunca aterrissa numa pagina 403 logo
 * apos digitar a senha certa.</p>
 */
@Component
public class RedirecionamentoPorPerfilHandler implements AuthenticationSuccessHandler {

    public static final String DESTINO_ADMIN = "/admin";
    public static final String DESTINO_CLIENTE = "/catalogo";

    private final RedirectStrategy redirecionador = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest requisicao,
                                        HttpServletResponse resposta,
                                        Authentication autenticacao) throws IOException {
        // Limpa um eventual erro de tentativa anterior guardado na sessao.
        var sessao = requisicao.getSession(false);
        if (sessao != null) {
            sessao.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }
        redirecionador.sendRedirect(requisicao, resposta, destinoPara(autenticacao));
    }

    /** Tambem usado por GET /login e GET /cadastro quando a pessoa ja esta logada. */
    public static String destinoPara(Authentication autenticacao) {
        boolean administrador = autenticacao.getAuthorities().stream()
                .anyMatch(permissao -> "ROLE_ADMIN".equals(permissao.getAuthority()));
        return administrador ? DESTINO_ADMIN : DESTINO_CLIENTE;
    }
}
