package br.com.fiap.mercadoexpress.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dados pessoais editaveis em /minha-conta. A unicidade do e-mail e conferida
 * no controller, porque aqui o e-mail atual da propria pessoa e permitido.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContaForm {

    @NotBlank(message = "Informe o seu nome")
    @Size(min = 2, max = 100, message = "O nome precisa ter entre 2 e 100 caracteres")
    private String nome;

    @NotBlank(message = "Informe o seu e-mail")
    @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Digite um e-mail válido, ex.: ana@email.com")
    @Size(max = 120, message = "O e-mail pode ter no máximo 120 caracteres")
    private String email;
}
