package br.com.fiap.mercadoexpress.security.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Decide para onde vai quem errou o login.
 *
 * <table>
 *   <caption>Destino por tipo de falha</caption>
 *   <tr><th>E-mail sem conta</th><td>/cadastro?email=...&amp;origem=login (Sign Up pre-preenchido)</td></tr>
 *   <tr><th>Conta desativada</th><td>/login?inativo</td></tr>
 *   <tr><th>Senha errada</th><td>/login?erro&amp;email=... (o e-mail volta preenchido)</td></tr>
 * </table>
 *
 * <p>E a implementacao literal do requisito "usuario sem login e redirecionado
 * para a tela de cadastro": quem tenta entrar sem ter conta nao ve um "usuario
 * ou senha invalidos" generico, e sim o formulario de cadastro com o e-mail que
 * acabou de digitar. So funciona porque o DaoAuthenticationProvider esta com
 * {@code setHideUserNotFoundExceptions(false)}.</p>
 */
@Slf4j
@Component
public class CadastroSugeridoFailureHandler implements AuthenticationFailureHandler {

    private final RedirectStrategy redirecionador = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationFailure(HttpServletRequest requisicao,
                                        HttpServletResponse resposta,
                                        AuthenticationException falha) throws IOException {

        String email = emailDigitado(requisicao);
        String destino;

        if (falha instanceof UsernameNotFoundException) {
            log.info("Login com e-mail sem conta; redirecionando para o cadastro.");
            destino = comEmail(UriComponentsBuilder.fromPath("/cadastro").queryParam("origem", "login"), email);
        } else if (falha instanceof DisabledException) {
            destino = "/login?inativo";
        } else {
            destino = comEmail(UriComponentsBuilder.fromPath("/login").queryParam("erro"), email);
        }

        // DefaultRedirectStrategy acrescenta o context path e, atras do proxy,
        // respeita o X-Forwarded-* (server.forward-headers-strategy=framework).
        redirecionador.sendRedirect(requisicao, resposta, destino);
    }

    private String emailDigitado(HttpServletRequest requisicao) {
        String email = requisicao.getParameter("email");
        return email == null ? "" : email.trim();
    }

    /**
     * Acrescenta o e-mail como variavel de URI: assim ele e codificado de forma
     * estrita (o "+" de "ana+feira@x.com" vira %2B em vez de virar espaco).
     */
    private String comEmail(UriComponentsBuilder base, String email) {
        if (email.isEmpty()) {
            return base.build().toUriString();
        }
        return base.queryParam("email", "{email}").encode().buildAndExpand(email).toUriString();
    }
}
