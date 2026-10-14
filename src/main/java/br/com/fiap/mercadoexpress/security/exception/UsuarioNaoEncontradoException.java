package br.com.fiap.mercadoexpress.security.exception;

/** Id de usuario inexistente na URL do painel; vira pagina 404. */
public class UsuarioNaoEncontradoException extends RuntimeException {

    public UsuarioNaoEncontradoException(Long id) {
        super("Não existe usuário com o código " + id + ".");
    }
}
