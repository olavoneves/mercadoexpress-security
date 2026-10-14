package br.com.fiap.mercadoexpress.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Sobe o contexto inteiro para garantir que controllers, services, repositories
 * e a cadeia do Spring Security estao coerentes.
 *
 * <p>Roda no perfil dev (H2 em memoria) de proposito: assim o
 * {@code mvn clean package} passa em qualquer maquina, sem exigir o PostgreSQL.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
class MercadoexpressSecurityApplicationTests {

	@Test
	void contextLoads() {
	}

}
