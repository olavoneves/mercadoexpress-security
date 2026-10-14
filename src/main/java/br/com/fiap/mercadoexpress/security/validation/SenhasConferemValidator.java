package br.com.fiap.mercadoexpress.security.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Implementacao de {@link SenhasConferem}. */
public class SenhasConferemValidator implements ConstraintValidator<SenhasConferem, ComConfirmacaoDeSenha> {

    @Override
    public boolean isValid(ComConfirmacaoDeSenha formulario, ConstraintValidatorContext contexto) {
        if (formulario == null) {
            return true;
        }
        String senha = formulario.getSenha();
        String confirmacao = formulario.getConfirmacaoSenha();

        // Campo vazio e assunto do @NotBlank: nao duplica a mensagem.
        if (senha == null || senha.isEmpty() || confirmacao == null || confirmacao.isEmpty()) {
            return true;
        }
        if (senha.equals(confirmacao)) {
            return true;
        }

        contexto.disableDefaultConstraintViolation();
        contexto.buildConstraintViolationWithTemplate(contexto.getDefaultConstraintMessageTemplate())
                .addPropertyNode("confirmacaoSenha")
                .addConstraintViolation();
        return false;
    }
}
