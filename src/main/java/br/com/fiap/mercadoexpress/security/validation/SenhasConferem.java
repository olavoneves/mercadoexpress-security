package br.com.fiap.mercadoexpress.security.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricao de classe: a senha e a confirmacao precisam ser iguais.
 *
 * <p>Vale para qualquer formulario que implemente {@link ComConfirmacaoDeSenha}
 * (cadastro e troca de senha). O erro e pendurado no campo
 * {@code confirmacaoSenha}, para aparecer embaixo dele no Thymeleaf.</p>
 */
@Documented
@Constraint(validatedBy = SenhasConferemValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhasConferem {

    String message() default "A confirmação não bate com a senha digitada";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
