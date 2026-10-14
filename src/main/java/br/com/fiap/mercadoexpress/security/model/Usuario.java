package br.com.fiap.mercadoexpress.security.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Conta de acesso ao Mercado Express, gravada em TDS_USERS_MERCADO.
 *
 * <p>O e-mail e o login (username do Spring Security) e e unico no banco. A
 * senha nunca e gravada em texto puro: chega aqui ja como hash BCrypt.</p>
 *
 * <p>A validacao de formulario fica nos DTOs do pacote {@code dto}, e nao aqui,
 * porque cada tela (cadastro, minha conta, troca de senha) valida um recorte
 * diferente dos campos.</p>
 */
@Entity
@Table(name = "TDS_USERS_MERCADO")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "senha")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "NOME", nullable = false, length = 100)
    private String nome;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 120)
    private String email;

    /** Hash BCrypt ($2a$10$...), sempre com 60 caracteres. */
    @Column(name = "SENHA", nullable = false, length = 100)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(name = "PERFIL", nullable = false, length = 20)
    private Perfil perfil;

    @Column(name = "DATA_CADASTRO", nullable = false)
    private LocalDateTime dataCadastro;

    /** Conta desativada nao consegue autenticar (DisabledException). */
    @Column(name = "ATIVO", nullable = false)
    private Boolean ativo;

    public Usuario(String nome, String email, String senhaCriptografada, Perfil perfil) {
        this.nome = nome;
        this.email = email;
        this.senha = senhaCriptografada;
        this.perfil = perfil;
    }

    @PrePersist
    private void aoCadastrar() {
        if (dataCadastro == null) {
            dataCadastro = LocalDateTime.now();
        }
        if (ativo == null) {
            ativo = Boolean.TRUE;
        }
        if (perfil == null) {
            perfil = Perfil.CLIENTE;
        }
    }

    public boolean isAdmin() {
        return perfil == Perfil.ADMIN;
    }
}
