package br.com.fiap.mercadoexpress.security.service;

import br.com.fiap.mercadoexpress.security.dto.CadastroForm;
import br.com.fiap.mercadoexpress.security.dto.ContaForm;
import br.com.fiap.mercadoexpress.security.dto.SenhaForm;
import br.com.fiap.mercadoexpress.security.exception.UsuarioNaoEncontradoException;
import br.com.fiap.mercadoexpress.security.model.Perfil;
import br.com.fiap.mercadoexpress.security.model.Usuario;
import br.com.fiap.mercadoexpress.security.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras das contas de usuario: cadastro, edicao da propria conta, troca de
 * senha e promocao a administrador.
 *
 * <p>Toda senha passa pelo {@link PasswordEncoder} (BCrypt) antes de tocar o
 * banco, e todo e-mail e gravado aparado e em minusculas, para que
 * "Ana@Email.com " e "ana@email.com" sejam a mesma conta.</p>
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return repository.findAllByOrderByPerfilAscNomeAsc();
    }

    @Transactional(readOnly = true)
    public long contarPorPerfil(Perfil perfil) {
        return repository.countByPerfil(perfil);
    }

    /** Na edicao da conta: o e-mail digitado ja pertence a OUTRA pessoa? */
    @Transactional(readOnly = true)
    public boolean emailEmUsoPorOutro(String email, Long idDoDono) {
        return repository.existsByEmailIgnoreCaseAndIdNot(normalizarEmail(email), idDoDono);
    }

    // ------------------------------------------------------------------
    // Comandos
    // ------------------------------------------------------------------

    /** Sign Up: todo cadastro pela tela publica nasce com o perfil CLIENTE. */
    @Transactional
    public Usuario cadastrarCliente(CadastroForm formulario) {
        Usuario novo = new Usuario(
                formulario.getNome().trim(),
                normalizarEmail(formulario.getEmail()),
                encoder.encode(formulario.getSenha()),
                Perfil.CLIENTE);
        return repository.save(novo);
    }

    /** Minha conta: nome e e-mail. O perfil nunca e alterado por aqui. */
    @Transactional
    public Usuario atualizarDados(Long id, ContaForm formulario) {
        Usuario usuario = buscarPorId(id);
        usuario.setNome(formulario.getNome().trim());
        usuario.setEmail(normalizarEmail(formulario.getEmail()));
        return repository.save(usuario);
    }

    /**
     * Troca de senha. Devolve {@code false} se a senha atual nao confere, para
     * o controller apontar o erro no campo certo.
     */
    @Transactional
    public boolean trocarSenha(Long id, SenhaForm formulario) {
        Usuario usuario = buscarPorId(id);
        if (!encoder.matches(formulario.getSenhaAtual(), usuario.getSenha())) {
            return false;
        }
        usuario.setSenha(encoder.encode(formulario.getSenha()));
        repository.save(usuario);
        return true;
    }

    /** Painel /admin/usuarios: promove um cliente a ADMIN. */
    @Transactional
    public Usuario promoverAdmin(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.setPerfil(Perfil.ADMIN);
        return repository.save(usuario);
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
