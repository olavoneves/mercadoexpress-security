package br.com.fiap.mercadoexpress.security.auth;

import br.com.fiap.mercadoexpress.security.model.Perfil;
import br.com.fiap.mercadoexpress.security.model.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

/**
 * Principal guardado na sessao depois do login.
 *
 * <p>Estende o {@link User} do Spring Security (username = e-mail, senha =
 * hash BCrypt, authority = ROLE_CLIENTE ou ROLE_ADMIN) e acrescenta o que as
 * telas precisam mostrar sem ir ao banco: id, nome e perfil. No Thymeleaf o
 * cabecalho le {@code #authentication.principal.nome}.</p>
 */
public class UsuarioAutenticado extends User {

    private final Long id;
    private final String nome;
    private final Perfil perfil;

    public UsuarioAutenticado(Usuario usuario) {
        super(usuario.getEmail(),
                usuario.getSenha(),
                Boolean.TRUE.equals(usuario.getAtivo()),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority(usuario.getPerfil().getAuthority())));
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.perfil = usuario.getPerfil();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    /** Primeiro nome, para a saudacao do cabecalho. */
    public String getPrimeiroNome() {
        return nome.trim().split("\\s+")[0];
    }

    /** Inicial usada no "avatar" do cabecalho e da pagina Minha conta. */
    public String getInicial() {
        return nome.trim().substring(0, 1).toUpperCase();
    }

    public boolean isAdmin() {
        return perfil == Perfil.ADMIN;
    }
}
