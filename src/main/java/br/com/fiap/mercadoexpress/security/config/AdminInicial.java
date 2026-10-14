package br.com.fiap.mercadoexpress.security.config;

import br.com.fiap.mercadoexpress.security.model.Perfil;
import br.com.fiap.mercadoexpress.security.model.Usuario;
import br.com.fiap.mercadoexpress.security.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garante que exista um administrador logo no primeiro startup.
 *
 * <p>O admin nao vem do script SQL de proposito: o hash BCrypt muda a cada
 * geracao (salt aleatorio) e precisa ser produzido pelo mesmo
 * {@link PasswordEncoder} que a aplicacao usa no login. Se o e-mail ja existir,
 * nada e alterado - reiniciar a aplicacao nunca sobrescreve a senha.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInicial implements CommandLineRunner {

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    @Value("${app.admin.nome}")
    private String nome;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.senha}")
    private String senha;

    @Override
    @Transactional
    public void run(String... args) {
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            log.warn("app.admin.email/app.admin.senha vazios: administrador inicial NAO foi criado.");
            return;
        }

        String emailNormalizado = email.trim().toLowerCase();
        if (repository.existsByEmailIgnoreCase(emailNormalizado)) {
            log.info("Administrador inicial {} ja existe.", emailNormalizado);
            return;
        }

        repository.save(new Usuario(nome, emailNormalizado, encoder.encode(senha), Perfil.ADMIN));
        log.info("Administrador inicial {} criado.", emailNormalizado);
    }
}
