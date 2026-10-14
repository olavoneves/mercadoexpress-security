package br.com.fiap.mercadoexpress.security.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * O e-mail ainda nao pode ter conta em TDS_USERS_MERCADO.
 *
 * <p>O validador consulta o banco: o Spring injeta o repositorio nele porque o
 * Hibernate Validator do Spring Boot usa o {@code SpringConstraintValidatorFactory}.
 * A coluna EMAIL tambem e UNIQUE no banco, que segura uma eventual corrida
 * entre dois cadastros simultaneos.</p>
 */
@Documented
@Constraint(validatedBy = EmailDisponivelValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EmailDisponivel {

    String message() default "Já existe uma conta com este e-mail. Entre com a sua senha.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
