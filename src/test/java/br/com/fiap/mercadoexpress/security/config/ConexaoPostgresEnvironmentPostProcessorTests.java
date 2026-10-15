package br.com.fiap.mercadoexpress.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Formatos de URL que o Render e o Neon entregam. */
class ConexaoPostgresEnvironmentPostProcessorTests {

    private final ConexaoPostgresEnvironmentPostProcessor processador = new ConexaoPostgresEnvironmentPostProcessor();

    @Test
    void urlInternaDoRenderSemPortaViraJdbc() {
        MockEnvironment ambiente = new MockEnvironment()
                .withProperty("DATABASE_URL", "postgres://mercado:s3nh%40forte@dpg-abc123-a/mercadoexpress");

        processador.postProcessEnvironment(ambiente, new SpringApplication());

        assertThat(ambiente.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://dpg-abc123-a:5432/mercadoexpress");
        assertThat(ambiente.getProperty("spring.datasource.username")).isEqualTo("mercado");
        assertThat(ambiente.getProperty("spring.datasource.password")).isEqualTo("s3nh@forte");
    }

    @Test
    void dbUrlNoFormatoPostgresComSslDoNeon() {
        MockEnvironment ambiente = new MockEnvironment()
                .withProperty("DB_URL", "postgresql://u:p@ep-x.neon.tech/neondb?sslmode=require");

        processador.postProcessEnvironment(ambiente, new SpringApplication());

        assertThat(ambiente.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://ep-x.neon.tech:5432/neondb?sslmode=require");
    }

    @Test
    void jdbcPassaIntactoEUsuarioExplicitoTemPrioridade() {
        MockEnvironment ambiente = new MockEnvironment()
                .withProperty("DB_URL", "jdbc:postgresql://host:5432/banco")
                .withProperty("DB_USER", "explicito");

        processador.postProcessEnvironment(ambiente, new SpringApplication());

        assertThat(ambiente.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://host:5432/banco");
        assertThat(ambiente.getPropertySources().get(ConexaoPostgresEnvironmentPostProcessor.FONTE)
                .containsProperty("spring.datasource.username")).isFalse();
    }

    @Test
    void noRenderSemUrlFalhaComMensagemClara() {
        MockEnvironment ambiente = new MockEnvironment().withProperty("RENDER", "true");

        assertThatThrownBy(() -> processador.postProcessEnvironment(ambiente, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("defina a variavel DB_URL");
    }

    @Test
    void localSemUrlMantemODefaultDoCompose() {
        MockEnvironment ambiente = new MockEnvironment();

        processador.postProcessEnvironment(ambiente, new SpringApplication());

        assertThat(ambiente.getPropertySources().contains(ConexaoPostgresEnvironmentPostProcessor.FONTE)).isFalse();
    }
}
