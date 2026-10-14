package br.com.fiap.mercadoexpress.security.repository;

import br.com.fiap.mercadoexpress.security.model.Perfil;
import br.com.fiap.mercadoexpress.security.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Acesso a TDS_USERS_MERCADO. O e-mail e sempre gravado em minusculas, mas as
 * buscas ignoram caixa para nao depender disso.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Usado pelo UserDetailsService no momento do login. */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Na edicao da propria conta: o e-mail pode ser o dele mesmo, nao o de outro. */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Usuario> findAllByOrderByPerfilAscNomeAsc();

    long countByPerfil(Perfil perfil);
}
