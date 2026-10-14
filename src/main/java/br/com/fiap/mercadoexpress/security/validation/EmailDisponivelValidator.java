package br.com.fiap.mercadoexpress.security.validation;

import br.com.fiap.mercadoexpress.security.repository.UsuarioRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

/** Implementacao de {@link EmailDisponivel}. */
@RequiredArgsConstructor
public class EmailDisponivelValidator implements ConstraintValidator<EmailDisponivel, String> {

    private final UsuarioRepository repository;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext contexto) {
        // Vazio ou mal formado e assunto do @NotBlank / @Email.
        if (email == null || email.isBlank()) {
            return true;
        }
        return !repository.existsByEmailIgnoreCase(email.trim());
    }
}
