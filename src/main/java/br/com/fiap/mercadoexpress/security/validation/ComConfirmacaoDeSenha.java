package br.com.fiap.mercadoexpress.security.validation;

/** Formulario que pede a senha duas vezes; validado por {@link SenhasConferem}. */
public interface ComConfirmacaoDeSenha {

    String getSenha();

    String getConfirmacaoSenha();
}
