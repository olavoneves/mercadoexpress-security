package br.com.fiap.mercadoexpress.security.dto;

import br.com.fiap.mercadoexpress.security.validation.ComConfirmacaoDeSenha;
import br.com.fiap.mercadoexpress.security.validation.SenhasConferem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Troca de senha em /minha-conta: exige a senha atual e a nova duas vezes. */
@Getter
@Setter
@NoArgsConstructor
@SenhasConferem(message = "A confirmação não bate com a nova senha")
public class SenhaForm implements ComConfirmacaoDeSenha {

    @NotBlank(message = "Informe a sua senha atual")
    private String senhaAtual;

    /** Nova senha. */
    @NotBlank(message = "Digite a nova senha")
    @Size(min = 6, max = 72, message = "A nova senha precisa ter no mínimo 6 caracteres")
    private String senha;

    @NotBlank(message = "Repita a nova senha para confirmar")
    private String confirmacaoSenha;
}
