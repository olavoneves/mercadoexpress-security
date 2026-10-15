package br.com.fiap.mercadoexpress.security.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Traduz a URL de conexao que os provedores de nuvem entregam para o formato
 * JDBC que o Spring espera, antes de o DataSource ser criado.
 *
 * <p>O Render (e o Neon) mostram a conexao como
 * {@code postgres://usuario:senha@host:5432/banco}, enquanto o driver JDBC
 * exige {@code jdbc:postgresql://host:5432/banco} com usuario e senha
 * separados. Aqui aceitamos qualquer um dos dois formatos, vindo de
 * {@code DB_URL} ou de {@code DATABASE_URL}.</p>
 *
 * <p>No Render (variavel {@code RENDER} presente) sem nenhuma URL definida, a
 * aplicacao para com uma mensagem clara, em vez de cair no default
 * {@code localhost:5433} do docker compose e falhar com "connection refused".</p>
 *
 * <p>Registrado em META-INF/spring.factories.</p>
 */
public class ConexaoPostgresEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String FONTE = "conexaoPostgresTraduzida";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment ambiente, SpringApplication aplicacao) {
        if (ambiente.matchesProfiles("dev")) {
            return;
        }

        String url = primeiroPreenchido(ambiente.getProperty("DB_URL"), ambiente.getProperty("DATABASE_URL"));

        if (url == null) {
            if (ambiente.getProperty("RENDER") != null) {
                throw new IllegalStateException("""
                        Banco nao configurado no Render: defina a variavel DB_URL (ou DATABASE_URL) \
                        no Environment do web service. Pode colar a "Internal Database URL" do \
                        Postgres do Render (postgres://...) ou uma URL JDBC (jdbc:postgresql://...).""");
            }
            return; // local: vale o default do application.yml (Postgres do docker compose)
        }

        Map<String, Object> propriedades = new HashMap<>();
        Conexao conexao = traduzir(url);
        propriedades.put("spring.datasource.url", conexao.jdbcUrl());

        // DB_USER/DB_PASSWORD explicitos tem prioridade sobre o que vem na URL.
        if (conexao.usuario() != null && primeiroPreenchido(ambiente.getProperty("DB_USER")) == null) {
            propriedades.put("spring.datasource.username", conexao.usuario());
        }
        if (conexao.senha() != null && primeiroPreenchido(ambiente.getProperty("DB_PASSWORD")) == null) {
            propriedades.put("spring.datasource.password", conexao.senha());
        }

        ambiente.getPropertySources().addFirst(new MapPropertySource(FONTE, propriedades));
    }

    /** Converte postgres:// ou postgresql:// em JDBC; URL que ja e JDBC passa intacta. */
    static Conexao traduzir(String url) {
        String limpa = url.trim();
        if (limpa.startsWith("jdbc:")) {
            return new Conexao(limpa, null, null);
        }
        if (!limpa.startsWith("postgres://") && !limpa.startsWith("postgresql://")) {
            throw new IllegalStateException(
                    "URL do banco em formato desconhecido. Use jdbc:postgresql://... ou postgres://...");
        }

        URI uri = URI.create(limpa);
        int porta = uri.getPort() == -1 ? 5432 : uri.getPort();
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost()).append(':').append(porta).append(uri.getRawPath());
        if (uri.getRawQuery() != null) {
            jdbc.append('?').append(uri.getRawQuery());
        }

        String usuario = null;
        String senha = null;
        if (uri.getRawUserInfo() != null) {
            String[] partes = uri.getRawUserInfo().split(":", 2);
            usuario = decodificar(partes[0]);
            senha = partes.length > 1 ? decodificar(partes[1]) : null;
        }
        return new Conexao(jdbc.toString(), usuario, senha);
    }

    private static String decodificar(String valor) {
        return URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }

    private static String primeiroPreenchido(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }

    /** Depois do ConfigData, para ja enxergar os perfis ativos. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    record Conexao(String jdbcUrl, String usuario, String senha) {
    }
}
