package br.com.fiap.mercadoexpress.security.model;

/**
 * Tipos de usuario do Mercado Express. O nome da constante e gravado na coluna
 * PERFIL de TDS_USERS_MERCADO e vira a authority {@code ROLE_<NOME>} que o
 * Spring Security confere nas rotas.
 */
public enum Perfil {

    /** Perfil padrao de quem se cadastra: catalogo, detalhe e a propria conta. */
    CLIENTE("Cliente"),

    /** Equipe do mercado: tudo do cliente mais o painel /admin. */
    ADMIN("Administrador");

    private final String rotulo;

    Perfil(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Authority no formato que {@code hasRole(...)} espera. */
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
