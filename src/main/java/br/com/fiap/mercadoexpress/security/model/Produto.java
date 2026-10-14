package br.com.fiap.mercadoexpress.security.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Produto exposto no catalogo do Mercado Express.
 *
 * <p>Mapeada na tabela TDS_SEC_MVC_TB_MERCADO do PostgreSQL. As colunas sao as
 * mesmas do Checkpoint 4; a unica mudanca e a coluna ATIVO, que no Oracle era
 * CHAR(1) 'S'/'N' e no PostgreSQL passa a ser BOOLEAN nativo.</p>
 *
 * <p>Esta mesma classe serve de objeto de formulario no Thymeleaf, por isso
 * carrega as anotacoes de Bean Validation.</p>
 */
@Entity
@Table(name = "TDS_SEC_MVC_TB_MERCADO")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Produto {

    /** GENERATED ALWAYS AS IDENTITY no banco: quem numera e o PostgreSQL. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Informe o nome do produto")
    @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres")
    @Column(name = "NOME", nullable = false, length = 100)
    private String nome;

    @NotBlank(message = "Informe o tipo do produto")
    @Size(max = 50, message = "O tipo pode ter no máximo 50 caracteres")
    @Column(name = "TIPO", nullable = false, length = 50)
    private String tipo;

    @NotBlank(message = "Escolha um setor")
    @Size(max = 50, message = "O setor pode ter no máximo 50 caracteres")
    @Column(name = "SETOR", nullable = false, length = 50)
    private String setor;

    @Size(max = 30, message = "O tamanho pode ter no máximo 30 caracteres")
    @Column(name = "TAMANHO", length = 30)
    private String tamanho;

    @NotNull(message = "Informe o preço")
    @Positive(message = "O preço precisa ser maior que zero")
    @DecimalMax(value = "99999999.99", message = "O preço máximo aceito é R$ 99.999.999,99")
    @Column(name = "PRECO", nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres")
    @Column(name = "DESCRICAO", length = 500)
    private String descricao;

    @NotNull(message = "Informe a quantidade em estoque")
    @PositiveOrZero(message = "O estoque não pode ser negativo")
    @Column(name = "ESTOQUE", nullable = false)
    private Integer estoque;

    /** Produto inativo continua no banco, mas some do catalogo. */
    @Column(name = "ATIVO", nullable = false)
    private Boolean ativo;

    @Column(name = "DATA_CADASTRO", nullable = false)
    private LocalDate dataCadastro;

    /** Blinda o banco caso o formulario nao envie os campos de controle. */
    @PrePersist
    private void aoCadastrar() {
        if (dataCadastro == null) {
            dataCadastro = LocalDate.now();
        }
        if (ativo == null) {
            ativo = Boolean.TRUE;
        }
    }

    /** Usado pelo catalogo para marcar o card de produto sem estoque. */
    public boolean isEsgotado() {
        return estoque == null || estoque <= 0;
    }
}
