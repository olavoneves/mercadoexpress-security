package br.com.fiap.mercadoexpress.security.auth;

import br.com.fiap.mercadoexpress.security.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ponte entre o Spring Security e a tabela TDS_USERS_MERCADO.
 *
 * <p>No POST /login o {@code DaoAuthenticationProvider} chama este metodo com o
 * e-mail digitado, recebe o hash BCrypt guardado no banco e compara com a
 * senha informada. Se o e-mail nao existir, a {@link UsernameNotFoundException}
 * chega intacta ao {@link CadastroSugeridoFailureHandler} (ver
 * {@code setHideUserNotFoundExceptions(false)} no SecurityConfig), que manda a
 * pessoa para a tela de cadastro.</p>
 */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String emailNormalizado = email == null ? "" : email.trim();
        return repository.findByEmailIgnoreCase(emailNormalizado)
                .map(UsuarioAutenticado::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Nenhuma conta cadastrada com o e-mail " + emailNormalizado));
    }
}
