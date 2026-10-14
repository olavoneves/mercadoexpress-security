package br.com.fiap.mercadoexpress.security;

import br.com.fiap.mercadoexpress.security.auth.UsuarioAutenticado;
import br.com.fiap.mercadoexpress.security.model.Perfil;
import br.com.fiap.mercadoexpress.security.model.Usuario;
import br.com.fiap.mercadoexpress.security.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Percorre o fluxo de seguranca de ponta a ponta, do jeito que o navegador faz:
 * landing publica, rota protegida, login com e-mail inexistente caindo no
 * cadastro, cadastro, login por perfil, 403 e logout.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class FluxoDeAutenticacaoTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PasswordEncoder encoder;

    @Test
    void landingEPublica() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("destaques"));
    }

    @Test
    void catalogoSemLoginVaiParaOLogin() throws Exception {
        mvc.perform(get("/catalogo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?restrito"));
    }

    @Test
    void loginComEmailInexistenteVaiParaOCadastroPreenchido() throws Exception {
        mvc.perform(post("/login").with(csrf())
                        .param("email", "nova+feira@email.com")
                        .param("senha", "qualquer"))
                .andExpect(redirectedUrl("/cadastro?origem=login&email=nova%2Bfeira%40email.com"));

        mvc.perform(get("/cadastro").param("email", "nova+feira@email.com").param("origem", "login"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("vindoDoLogin", true));
    }

    @Test
    void senhaErradaDeEmailExistenteVoltaParaOLogin() throws Exception {
        mvc.perform(post("/login").with(csrf())
                        .param("email", "admin@mercado.com")
                        .param("senha", "errada"))
                .andExpect(redirectedUrl("/login?erro&email=admin%40mercado.com"));
    }

    @Test
    void adminVaiParaOPainelEClienteParaOCatalogo() throws Exception {
        mvc.perform(post("/login").with(csrf())
                        .param("email", "admin@mercado.com")
                        .param("senha", "admin123"))
                .andExpect(redirectedUrl("/admin"));

        usuarios.save(new Usuario("Cliente Teste", "cliente@teste.com", encoder.encode("123456"), Perfil.CLIENTE));
        mvc.perform(post("/login").with(csrf())
                        .param("email", "cliente@teste.com")
                        .param("senha", "123456"))
                .andExpect(redirectedUrl("/catalogo"));
    }

    @Test
    void cadastroValidoGravaClienteComBcryptEVaiParaOLogin() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Ana Souza")
                        .param("email", "Ana@Email.com")
                        .param("senha", "segredo1")
                        .param("confirmacaoSenha", "segredo1"))
                .andExpect(redirectedUrl("/login?email=ana%40email.com"));

        Usuario salvo = usuarios.findByEmailIgnoreCase("ana@email.com").orElseThrow();
        assertThat(salvo.getPerfil()).isEqualTo(Perfil.CLIENTE);
        assertThat(salvo.getEmail()).isEqualTo("ana@email.com");
        assertThat(salvo.getSenha()).startsWith("$2").isNotEqualTo("segredo1");
        assertThat(encoder.matches("segredo1", salvo.getSenha())).isTrue();

        mvc.perform(post("/login").with(csrf())
                        .param("email", "ana@email.com")
                        .param("senha", "segredo1"))
                .andExpect(redirectedUrl("/catalogo"));
    }

    @Test
    void cadastroInvalidoMostraErroPorCampo() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "A")
                        .param("email", "admin@mercado.com")
                        .param("senha", "123")
                        .param("confirmacaoSenha", "456"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/cadastro"))
                .andExpect(model().attributeHasFieldErrors("cadastro", "nome", "email", "senha"));

        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Bruno")
                        .param("email", "email-invalido")
                        .param("senha", "123456")
                        .param("confirmacaoSenha", "654321"))
                .andExpect(model().attributeHasFieldErrors("cadastro", "email", "confirmacaoSenha"));
    }

    @Test
    void clienteNoPainelRecebe403() throws Exception {
        Usuario cliente = usuarios.save(
                new Usuario("Carla", "carla@teste.com", encoder.encode("123456"), Perfil.CLIENTE));

        mvc.perform(get("/admin").with(user(new UsuarioAutenticado(cliente))))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acesso-negado"));

        mvc.perform(get("/catalogo").with(user(new UsuarioAutenticado(cliente))))
                .andExpect(status().isOk());
    }

    @Test
    void postSemTokenCsrfEBloqueado() throws Exception {
        mvc.perform(post("/cadastro")
                        .param("nome", "Sem Token")
                        .param("email", "sem@token.com")
                        .param("senha", "123456")
                        .param("confirmacaoSenha", "123456"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutPorPostVoltaParaOLogin() throws Exception {
        mvc.perform(post("/logout").with(csrf()).with(user("admin@mercado.com").roles("ADMIN")))
                .andExpect(redirectedUrl("/login?logout"));
    }
}
