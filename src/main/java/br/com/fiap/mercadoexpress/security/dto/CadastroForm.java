package br.com.fiap.mercadoexpress.security.dto;

import br.com.fiap.mercadoexpress.security.validation.ComConfirmacaoDeSenha;
import br.com.fiap.mercadoexpress.security.validation.EmailDisponivel;
import br.com.fiap.mercadoexpress.security.validation.SenhasConferem;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Formulario da tela de cadastro (Sign Up). Toda regra e Bean Validation:
 * e-mail valido e livre, senha com no minimo 6 caracteres e confirmacao igual.
 */
@Getter
@Setter
@NoArgsConstructor
@SenhasConferem
public class CadastroForm implements ComConfirmacaoDeSenha {

    @NotBlank(message = "Informe o seu nome")
    @Size(min = 2, max = 100, message = "O nome precisa ter entre 2 e 100 caracteres")
    private String nome;

    @NotBlank(message = "Informe o seu e-mail")
    @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Digite um e-mail válido, ex.: ana@email.com")
    @Size(max = 120, message = "O e-mail pode ter no máximo 120 caracteres")
    @EmailDisponivel
    private String email;

    /** 72 e o limite do BCrypt: bytes alem disso seriam ignorados no hash. */
    @NotBlank(message = "Crie uma senha")
    @Size(min = 6, max = 72, message = "A senha precisa ter no mínimo 6 caracteres")
    private String senha;

    @NotBlank(message = "Repita a senha para confirmar")
    private String confirmacaoSenha;
}
