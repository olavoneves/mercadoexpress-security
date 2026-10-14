package br.com.fiap.mercadoexpress.security.config;

import br.com.fiap.mercadoexpress.security.auth.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Configuracao do Spring Security do Mercado Express.
 *
 * <table>
 *   <caption>Mapa de acesso</caption>
 *   <tr><th>Publico</th><td>/, /login, /cadastro, /acesso-negado, /error e estaticos</td></tr>
 *   <tr><th>CLIENTE ou ADMIN</th><td>/catalogo, /produtos/{id}, /minha-conta</td></tr>
 *   <tr><th>ADMIN</th><td>/admin/** (CRUD de produtos e lista de usuarios)</td></tr>
 * </table>
 *
 * <p>Visitante anonimo numa rota protegida vai para /login?restrito. Usuario
 * autenticado sem o perfil exigido recebe a pagina 403 propria do projeto.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Cadeia unica. As regras sao lidas de cima para baixo e a primeira que
     * casar decide, por isso as rotas publicas vem antes das restritas.
     */
    @Bean
    SecurityFilterChain filtroPrincipal(HttpSecurity http,
                                        AuthenticationManager gerenciadorDeAutenticacao) throws Exception {
        http
                .authenticationManager(gerenciadorDeAutenticacao)

                .authorizeHttpRequests(rotas -> rotas
                        // ---------- Estaticos ----------
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                        // ---------- Publico ----------
                        .requestMatchers("/", "/login", "/cadastro", "/acesso-negado").permitAll()
                        // O Spring Boot despacha os erros para /error, e esse despacho tambem
                        // passa por esta cadeia. Sem liberar a rota, uma falha numa pagina
                        // publica virava redirect para /login - no CP4 isso gerou loop no Render.
                        .requestMatchers("/error").permitAll()
                        // ---------- Somente ADMIN ----------
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // ---------- CLIENTE ou ADMIN ----------
                        .requestMatchers("/catalogo", "/produtos/*", "/minha-conta", "/minha-conta/**")
                        .hasAnyRole("CLIENTE", "ADMIN")
                        // ---------- Qualquer outra coisa ----------
                        .anyRequest().authenticated())

                // Tela de login propria (templates/auth/login.html), no lugar da
                // pagina gerada pelo Spring Security.
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("senha")
                        .defaultSuccessUrl("/catalogo", true)
                        .failureUrl("/login?erro")
                        .permitAll())

                // Logout so por POST: o formulario do cabecalho envia o token CSRF.
                // CSRF fica no padrao do Spring (ativo); o Thymeleaf injeta o token
                // em todo <form th:action> automaticamente.
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())

                .exceptionHandling(erros -> erros
                        // Anonimo em rota protegida: login com aviso explicando o motivo.
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login?restrito"))
                        // Autenticado sem o perfil exigido: pagina 403 propria.
                        .accessDeniedPage("/acesso-negado"));

        return http.build();
    }

    /**
     * Gerenciador de autenticacao com um unico provedor: o
     * {@link DaoAuthenticationProvider}, que busca o usuario pelo
     * {@link UsuarioDetailsService} e confere a senha com o BCrypt.
     *
     * <p>{@code setHideUserNotFoundExceptions(false)} e a peca-chave do requisito
     * "sem login, redirecionar para o Sign Up": por padrao o Spring converte
     * "usuario nao existe" em BadCredentialsException, o que deixaria o failure
     * handler sem saber se o problema e o e-mail ou a senha. Desligando o
     * mascaramento, a UsernameNotFoundException chega ao handler e o visitante
     * sem conta e levado ao cadastro com o e-mail ja preenchido.</p>
     *
     * <p>Contrapartida consciente: a aplicacao passa a revelar se um e-mail tem
     * conta (enumeracao de usuarios). Para um mercado de bairro com cadastro
     * aberto, a experiencia de "nao achei sua conta, crie agora" vale mais. O
     * README discute o trade-off.</p>
     */
    @Bean
    AuthenticationManager gerenciadorDeAutenticacao(UsuarioDetailsService usuarios, PasswordEncoder encoder) {
        DaoAuthenticationProvider provedor = new DaoAuthenticationProvider(usuarios);
        provedor.setPasswordEncoder(encoder);
        provedor.setHideUserNotFoundExceptions(false);
        return new ProviderManager(provedor);
    }

    /** BCrypt com custo padrao (10): o hash ja carrega o salt embutido. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
