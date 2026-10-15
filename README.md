# Mercado Express · Spring Security MVC

> **FIAP · TDS · Checkpoint 5 · Parte I** (Spring Security MVC, Deploy e PostgreSQL)
> Prof. Dr. Marcel Stefan Wagner · Turma 2TDSR

O Mercado Express é a "feira de bairro" que acompanhamos desde o Checkpoint 4. Nesta etapa a aplicação ganhou **contas de usuário de verdade**: cadastro (Sign Up) gravado no PostgreSQL, **tela de login própria**, **dois perfis com acessos diferentes** (Cliente e Administrador), redirecionamento por perfil depois do login, landing page pública e deploy no Render.

**🔗 Deploy:** `[PREENCHER LINK]`  ·  **Credenciais de teste:** `admin@mercado.com` / `admin123` (cliente: crie pela tela de cadastro)

<!-- [SUBSTITUIR] Print da landing page no ar -->
![Landing page](docs/03-landing.png)

---

## Sumário

1. [Tecnologias](#tecnologias)
2. [Arquitetura](#arquitetura)
3. [Rotas por perfil](#rotas-por-perfil)
4. [⭐ Autenticação e tela de login](#-autenticação-e-tela-de-login)
5. [Landing page](#landing-page)
6. [CRUD de produtos](#crud-de-produtos)
7. [Banco de dados (PostgreSQL)](#banco-de-dados-postgresql)
8. [Como rodar](#como-rodar)
9. [Deploy](#deploy)
10. [Testes](#testes)
11. [Integrantes](#integrantes)

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.1 (Spring MVC, Spring Data JPA, Bean Validation) |
| Segurança | **Spring Security 7** + `thymeleaf-extras-springsecurity6` |
| Views | Thymeleaf (server-side) + CSS próprio, sem framework |
| Banco | **PostgreSQL 16** (H2 em memória no perfil `dev` e nos testes) |
| Build | Maven (wrapper incluso) |
| Infra | Docker (multi-stage), Docker Compose, Render |
| IDE | IntelliJ IDEA |

O projeto nasceu no Spring Initializr com Web, Thymeleaf, Security, Data JPA, PostgreSQL Driver, Lombok e Validation:

<!-- [SUBSTITUIR] Print do Spring Initializr -->
![Spring Initializr](docs/01-spring-initializr.png)

---

## Arquitetura

Camadas clássicas de Spring MVC, com a segurança como um filtro na frente de tudo:

```mermaid
flowchart LR
    B([Navegador]) -->|HTTP + cookie JSESSIONID + token CSRF| F

    subgraph SEC[Spring Security - SecurityFilterChain]
        F[Regras de rota<br/>por perfil] --> L[formLogin /login]
        L --> AM[AuthenticationManager<br/>DaoAuthenticationProvider]
        AM --> UDS[UsuarioDetailsService]
        L --> SH[RedirecionamentoPorPerfilHandler]
        L --> FH[CadastroSugeridoFailureHandler]
    end

    F --> C[Controllers<br/>Landing · Autenticacao · Catalogo<br/>Conta · AdminProduto · AdminUsuario]
    C --> S[Services<br/>ProdutoService · UsuarioService]
    S --> R[Repositories<br/>Spring Data JPA]
    UDS --> R
    R --> DB[(PostgreSQL<br/>TDS_SEC_MVC_TB_MERCADO<br/>TDS_USERS_MERCADO)]
    C --> V[Templates Thymeleaf<br/>+ app.css]
```

```
src/main/java/br/com/fiap/mercadoexpress/security
├── auth/          UsuarioAutenticado, UsuarioDetailsService, os 2 handlers de login
├── config/        SecurityConfig, AdminInicial (CommandLineRunner)
├── controller/    Landing, Autenticacao, Catalogo, Conta, AdminProduto, AdminUsuario, TratadorDeErros
├── dto/           CadastroForm, ContaForm, SenhaForm
├── exception/     ProdutoNaoEncontrado, UsuarioNaoEncontrado
├── model/         Produto, Usuario, Perfil (enum)
├── repository/    ProdutoRepository, UsuarioRepository
├── service/       ProdutoService, UsuarioService
└── validation/    @EmailDisponivel, @SenhasConferem (Bean Validation customizada)

src/main/resources
├── application.yml          perfis default (PostgreSQL) e dev (H2)
├── templates/
│   ├── index.html           landing page pública
│   ├── auth/                login.html, cadastro.html
│   ├── catalogo/            index.html, detalhe.html
│   ├── conta/               minha-conta.html
│   ├── admin/               painel, formulario, excluir, usuarios
│   ├── error/ + error.html  403, 404 e fallback
│   └── fragments/           layout (head, cabeçalho, faixa admin, rodapé), paginação
└── static/                  css/app.css, js/app.js, img/favicon.svg
```

---

## Rotas por perfil

Dois tipos de usuário (enum `Perfil`, coluna `PERFIL` de `TDS_USERS_MERCADO`), cada um vira uma *authority* do Spring Security:

| Rota | Visitante | `ROLE_CLIENTE` | `ROLE_ADMIN` |
|---|:---:|:---:|:---:|
| `/` landing page | ✅ | ✅ | ✅ |
| `/login`, `/cadastro` | ✅ | ↪ vai p/ `/catalogo` | ↪ vai p/ `/admin` |
| `/css/**`, `/js/**`, `/img/**`, `/error`, `/acesso-negado` | ✅ | ✅ | ✅ |
| `/catalogo`, `/produtos/{id}` | ↪ `/login?restrito` | ✅ | ✅ |
| `/minha-conta` (ver/editar nome, e-mail, senha) | ↪ `/login?restrito` | ✅ | ✅ |
| `/admin` (CRUD de produtos) | ↪ `/login?restrito` | ⛔ 403 | ✅ |
| `/admin/usuarios` (lista + promover a admin) | ↪ `/login?restrito` | ⛔ 403 | ✅ |
| **Destino após o login** | — | **`/catalogo`** | **`/admin`** |

O cabeçalho muda conforme o perfil (`sec:authorize`): visitante vê **Entrar / Criar conta**; logado vê **Catálogo**, **Painel** (só admin), o **nome + perfil** do usuário e o botão **Sair**.

<!-- [SUBSTITUIR] Print do cliente tentando acessar /admin (403) -->
![Cliente em /admin recebe 403](docs/10-cliente-admin-403.png)

---

## ⭐ Autenticação e tela de login

> Esta é a parte central do checkpoint. Tudo está em
> [`SecurityConfig`](src/main/java/br/com/fiap/mercadoexpress/security/config/SecurityConfig.java) e no pacote
> [`auth`](src/main/java/br/com/fiap/mercadoexpress/security/auth).

### 1. Tela de login própria

A página padrão do Spring Security foi substituída por [`templates/auth/login.html`](src/main/resources/templates/auth/login.html), com a identidade visual do projeto:

```java
.formLogin(login -> login
        .loginPage("/login")              // GET  -> nossa tela (AutenticacaoController)
        .loginProcessingUrl("/login")     // POST -> processado pelo filtro do Spring Security
        .usernameParameter("email")       // o e-mail é o login
        .passwordParameter("senha")
        .successHandler(aposLoginOk)      // destino por perfil
        .failureHandler(aposLoginFalho)   // e-mail sem conta -> cadastro
        .permitAll())
```

A tela trata cada situação com uma mensagem própria: `?restrito` (tentou abrir página protegida), `?erro` (senha errada, com o e-mail mantido no campo), `?logout`, `?inativo`, e o aviso verde "Conta criada!" vindo do cadastro.

<!-- [SUBSTITUIR] Print da tela de login -->
![Tela de login](docs/04-login.png)

### 2. "Sem login → tela de cadastro": o failure handler

O enunciado pede que o **usuário sem login seja redirecionado para o Sign Up**. Além do link "Criar conta" (na tela de login, no cabeçalho e na landing), implementamos o requisito de forma literal:

**quem tenta entrar com um e-mail que não tem conta é levado direto para `/cadastro`, com o e-mail já preenchido e uma mensagem explicando o motivo.** Senha errada para um e-mail que existe continua em `/login?erro`.

Isso é feito pelo [`CadastroSugeridoFailureHandler`](src/main/java/br/com/fiap/mercadoexpress/security/auth/CadastroSugeridoFailureHandler.java):

```java
if (falha instanceof UsernameNotFoundException) {
    destino = "/cadastro?origem=login&email=" + emailDigitado;   // Sign Up pré-preenchido
} else if (falha instanceof DisabledException) {
    destino = "/login?inativo";
} else {
    destino = "/login?erro&email=" + emailDigitado;               // senha errada
}
```

**O detalhe que faz funcionar:** por padrão, o Spring Security *esconde* o "usuário não encontrado" e transforma tudo em `BadCredentialsException`, para não revelar quais e-mails têm conta. Com isso o handler não conseguiria distinguir os dois casos. Por isso o `DaoAuthenticationProvider` é configurado com:

```java
DaoAuthenticationProvider provedor = new DaoAuthenticationProvider(usuarios);
provedor.setPasswordEncoder(encoder);
provedor.setHideUserNotFoundExceptions(false);   // deixa a UsernameNotFoundException chegar ao handler
```

> **Trade-off consciente.** Desligar esse mascaramento permite descobrir se um e-mail tem conta (*user enumeration*). Num sistema bancário isso seria inaceitável; num mercado de bairro com cadastro aberto, o próprio formulário de cadastro já revela isso ("Já existe uma conta com este e-mail"), e a experiência de "não achamos sua conta, crie agora" é exatamente o que o enunciado pede. Em produção real, mitigaríamos com limite de tentativas por IP (*rate limiting*).

<!-- [SUBSTITUIR] Print do login com e-mail inexistente redirecionando ao cadastro -->
![Login com e-mail inexistente vai para o cadastro](docs/05-login-email-inexistente-cadastro.png)

### 3. Cadastro (Sign Up) com Bean Validation

[`CadastroForm`](src/main/java/br/com/fiap/mercadoexpress/security/dto/CadastroForm.java) concentra as regras, todas declarativas:

| Campo | Regra | Anotação |
|---|---|---|
| nome | obrigatório, 2 a 100 caracteres | `@NotBlank` `@Size` |
| email | formato válido, máx. 120 | `@Email` `@Size` |
| email | **ainda não cadastrado** (consulta o banco) | `@EmailDisponivel` *(customizada)* |
| senha | mínimo 6 (máx. 72, limite do BCrypt) | `@NotBlank` `@Size(min = 6)` |
| confirmação | **igual à senha** | `@SenhasConferem` *(customizada, de classe)* |

As duas anotações customizadas ficam em [`validation/`](src/main/java/br/com/fiap/mercadoexpress/security/validation). O `@SenhasConferem` pendura o erro no campo `confirmacaoSenha`, então cada mensagem aparece **embaixo do campo certo** (`th:errors`) e também num resumo no topo do formulário. A senha nunca volta preenchida quando há erro.

Toda conta criada pela tela nasce como **`CLIENTE`**, é gravada em `TDS_USERS_MERCADO` e o usuário é levado ao login com o e-mail preenchido.

<!-- [SUBSTITUIR] Print do cadastro com erros de validação -->
![Cadastro com erros](docs/06-cadastro-erros.png)

<!-- [SUBSTITUIR] Print do login logo após o cadastro ("Conta criada!") -->
![Cadastro concluído](docs/07-cadastro-ok.png)

### 4. BCrypt e UserDetailsService próprio

- **Senha:** `BCryptPasswordEncoder` (custo 10, salt embutido no hash). No banco só existe `$2a$10$...`; nem o admin vê a senha.
- **[`UsuarioDetailsService`](src/main/java/br/com/fiap/mercadoexpress/security/auth/UsuarioDetailsService.java):** busca o usuário em `TDS_USERS_MERCADO` pelo e-mail (ignorando maiúsculas) e devolve um [`UsuarioAutenticado`](src/main/java/br/com/fiap/mercadoexpress/security/auth/UsuarioAutenticado.java), que estende o `User` do Spring com **id, nome e perfil**. É daí que o cabeçalho lê o nome (`#authentication.principal.nome`) sem ir ao banco a cada página.
- **Conta inativa** (`ATIVO = false`) não autentica (`DisabledException` → `/login?inativo`).
- **Admin inicial:** [`AdminInicial`](src/main/java/br/com/fiap/mercadoexpress/security/config/AdminInicial.java) (`CommandLineRunner`) cria `admin@mercado.com / admin123` no primeiro startup, se ainda não existir. Ele não vem do script SQL porque o hash BCrypt precisa ser gerado pelo mesmo `PasswordEncoder` da aplicação. E-mail e senha podem ser trocados pelas variáveis `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

### 5. Redirecionamento por perfil após o login

[`RedirecionamentoPorPerfilHandler`](src/main/java/br/com/fiap/mercadoexpress/security/auth/RedirecionamentoPorPerfilHandler.java) olha a *authority* de quem acabou de entrar:

- `ROLE_ADMIN` → **`/admin`** (painel do CRUD)
- `ROLE_CLIENTE` → **`/catalogo`**

Quem já está logado e abre `/login` ou `/cadastro` também é mandado para a sua área.

### 6. Fluxo completo

```mermaid
sequenceDiagram
    autonumber
    actor V as Visitante
    participant SEC as Spring Security
    participant UDS as UsuarioDetailsService
    participant DB as PostgreSQL
    participant FH as FailureHandler
    participant SH as SuccessHandler
    participant APP as Controllers

    V->>SEC: GET /catalogo (sem sessão)
    SEC-->>V: 302 /login?restrito
    V->>SEC: POST /login (email, senha, _csrf)
    SEC->>UDS: loadUserByUsername(email)
    UDS->>DB: SELECT ... FROM TDS_USERS_MERCADO WHERE email = ?

    alt e-mail não existe
        DB-->>UDS: vazio
        UDS-->>SEC: UsernameNotFoundException
        SEC->>FH: onAuthenticationFailure
        FH-->>V: 302 /cadastro?origem=login&email=...
        V->>APP: POST /cadastro (nome, email, senha, confirmação)
        APP->>APP: Bean Validation (@EmailDisponivel, @SenhasConferem...)
        APP->>DB: INSERT TDS_USERS_MERCADO (senha BCrypt, perfil CLIENTE)
        APP-->>V: 302 /login?email=... ("Conta criada!")
        V->>SEC: POST /login de novo
    else e-mail existe, senha errada
        SEC->>SEC: BCrypt.matches = false
        SEC->>FH: BadCredentialsException
        FH-->>V: 302 /login?erro&email=...
    end

    SEC->>SEC: BCrypt.matches = true
    SEC->>SH: onAuthenticationSuccess
    alt ROLE_ADMIN
        SH-->>V: 302 /admin
    else ROLE_CLIENTE
        SH-->>V: 302 /catalogo
    end
```

### 7. Outras proteções

| Proteção | Como |
|---|---|
| **CSRF** | Ativo (padrão). `th:action` injeta o token em todo formulário; POST sem token → 403 (coberto por teste). |
| **Logout só por POST** | O botão "Sair" é um `<form method="post">`; `GET /logout` não desloga. Sessão invalidada e `JSESSIONID` apagado. |
| **403 próprio** | Cliente em `/admin/**` vê `error/403.html` ("Essa porta é só da equipe"), não a página do Tomcat. |
| **`/error` liberado** | No CP4, sem isso, um erro numa página pública virava redirect para o login e gerava loop no Render. |
| **Proxy do Render** | `server.forward-headers-strategy: framework`: redirects saem em `https` com o host público. |
| **Minha conta** | O id vem sempre do usuário autenticado (`@AuthenticationPrincipal`), nunca da URL: um cliente não consegue editar a conta de outro. Trocar a senha exige a senha atual. |
| **Produto fora de linha** | Só o admin abre a ficha; para o cliente ele responde 404. |
| **Cookie de sessão** | `HttpOnly` e `SameSite=Lax`. |

<!-- [SUBSTITUIR] Print do catálogo logado como cliente (cabeçalho com nome e perfil) -->
![Catálogo como cliente](docs/08-catalogo-cliente.png)

<!-- [SUBSTITUIR] Print da tela Minha conta -->
![Minha conta](docs/09-minha-conta.png)

---

## Landing page

[`templates/index.html`](src/main/resources/templates/index.html) é pública e é a primeira impressão do projeto. Segue a identidade "etiqueta de feira" do CP4 (verde-folha, amarelo-etiqueta, Archivo condensada + Inter):

- **Abertura** com a proposta do mercado e os CTAs **Criar conta grátis** e **Entrar** (para quem já está logado, viram "Abrir o catálogo" / "Ir para o painel");
- **Quadro "Ofertas de hoje"**: uma lousa de feira com preços **vindos do banco**;
- faixa com os **setores** cadastrados;
- **produtos em destaque** (os ativos com estoque mais recentes, do PostgreSQL); a ficha completa pede login;
- **como funciona** em três passos e os **dois perfis** explicados (o que cada um acessa e para onde vai depois do login).

---

## CRUD de produtos

Área exclusiva do `ROLE_ADMIN`, toda operada por **links e botões** da interface. Tabela `TDS_SEC_MVC_TB_MERCADO`.

| Operação | Rota | Tela |
|---|---|---|
| **Read** (lista) | `GET /admin` | Tabela paginada com busca por nome, filtro por setor e contadores |
| **Create** | `GET /admin/produtos/novo` → `POST /admin/produtos` | Formulário com Bean Validation e erros por campo |
| **Update** | `GET /admin/produtos/{id}/editar` → `POST /admin/produtos/{id}` | Mesmo formulário, preenchido |
| **Delete** | `GET /admin/produtos/{id}/excluir` → `POST /admin/produtos/{id}/excluir` | Tela de confirmação; a exclusão é sempre POST com CSRF |
| Usuários | `GET /admin/usuarios` → `POST /admin/usuarios/{id}/promover` | Lista de contas e promoção de cliente a admin |

Detalhes: toda alteração termina em *redirect* com mensagem flash (F5 não reenvia o formulário); desmarcar "Exibir no catálogo" tira o produto do ar sem apagá-lo; a edição preserva id e data de cadastro.

<!-- [SUBSTITUIR] Print do painel administrativo -->
![Painel admin](docs/11-painel-admin.png)

<!-- [SUBSTITUIR] Print da lista de usuários -->
![Usuários](docs/12-usuarios.png)

<!-- [SUBSTITUIR] Print do formulário de cadastro de produto -->
![Cadastro de produto](docs/13-produto-cadastro.png)

<!-- [SUBSTITUIR] Print do formulário de edição -->
![Edição de produto](docs/14-produto-edicao.png)

<!-- [SUBSTITUIR] Print da tela de confirmação de exclusão -->
![Exclusão de produto](docs/15-produto-exclusao.png)

---

## Banco de dados (PostgreSQL)

O Oracle dos checkpoints anteriores foi trocado por **PostgreSQL**. As tabelas são criadas por [`database/script.sql`](database/script.sql) e a aplicação roda com `ddl-auto: validate`: o Hibernate não cria nada, só confere no startup se as entidades batem com o banco.

| Tabela | Colunas |
|---|---|
| `TDS_SEC_MVC_TB_MERCADO` | `ID` (identity), `NOME`, `TIPO`, `SETOR`, `TAMANHO`, `PRECO numeric(10,2)`, `DESCRICAO`, `ESTOQUE`, `ATIVO boolean`, `DATA_CADASTRO` |
| `TDS_USERS_MERCADO` | `ID` (identity), `NOME`, `EMAIL` (único, minúsculo), `SENHA` (hash BCrypt), `PERFIL` (`CLIENTE`/`ADMIN`), `DATA_CADASTRO`, `ATIVO` |

O script é idempotente (`DROP TABLE IF EXISTS`), usa `GENERATED ALWAYS AS IDENTITY`, constraints de `CHECK`/`UNIQUE`, comentários nas colunas e insere **13 produtos** (um deles inativo, para mostrar a diferença entre painel e catálogo). **Usuários não são inseridos pelo script** (veja o *Admin inicial* acima).

> Diferença do CP4: a coluna `ATIVO` era `CHAR(1)` `'S'/'N'` no Oracle e agora é `BOOLEAN` nativo, o que dispensou o `AttributeConverter`.

<!-- [SUBSTITUIR] Print das tabelas no PostgreSQL -->
![Tabelas no PostgreSQL](docs/02-tabelas-postgres.png)

---

## Como rodar

### Opção 1: Docker Compose (banco + aplicação)

Pré-requisito: Docker Desktop.

```bash
docker compose up -d --build
# http://localhost:8080   (admin@mercado.com / admin123)
```

- Sobe um **PostgreSQL 16** (porta **5433** no host, para não brigar com um Postgres local) e roda `database/script.sql` automaticamente na primeira vez.
- Constrói a imagem da aplicação e aponta `DB_URL` para o container do banco.
- Porta 8080 ocupada? `APP_PORT=8090 docker compose up -d`.
- Recomeçar do zero (apaga os dados): `docker compose down -v`.

### Opção 2: banco no Docker, aplicação no IntelliJ

```bash
docker compose up -d db
```

Depois rode `MercadoexpressSecurityApplication` pelo IntelliJ (ou `./mvnw spring-boot:run`). Sem variáveis definidas, o `application.yml` já aponta para o Postgres local do compose (`localhost:5433`, usuário/senha `mercado`, só desse banco descartável).

### Opção 3: sem Docker (H2 em memória)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Variáveis de ambiente

Veja [`.env.example`](.env.example). Nenhuma credencial real é versionada.

| Variável | Uso |
|---|---|
| `DB_URL` | JDBC do PostgreSQL, ex.: `jdbc:postgresql://host:5432/banco` |
| `DB_USER` / `DB_PASSWORD` | Credenciais do banco |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Admin criado no primeiro startup (padrão `admin@mercado.com` / `admin123`) |
| `SPRING_PROFILES_ACTIVE` | vazio = PostgreSQL · `dev` = H2 |
| `PORT` | Porta HTTP (o Render define sozinho) |

---

## Deploy

**Aplicação:** Render, *Web Service* com runtime **Docker** (o [`Dockerfile`](Dockerfile) é multi-stage: Maven compila, a imagem final só tem o JRE e roda sem root). O [`render.yaml`](render.yaml) descreve o serviço; as variáveis de banco e admin estão com `sync: false`, ou seja, são preenchidas no painel e **nunca ficam no GitHub**.

**Banco:** PostgreSQL gerenciado. Documentamos as duas opções:

| | Render Postgres | Neon |
|---|---|---|
| Plano gratuito | Expira em **30 dias** (suficiente para a correção) | **Permanente** (com pausa por inatividade) |
| Rede | Mesmo datacenter do app (URL interna) | Externo, exige `sslmode=require` |
| `DB_URL` | `jdbc:postgresql://dpg-xxxx-a:5432/mercadoexpress` | `jdbc:postgresql://ep-xxxx.sa-east-1.aws.neon.tech/neondb?sslmode=require` |

### Passo a passo

1. **Criar o banco**
   - *Render:* New → PostgreSQL → plano Free, mesma região do app. Em *Connections*, anote host interno, banco, usuário e senha.
   - *Neon:* crie o projeto em neon.tech e copie a *connection string* (formato JDBC).
2. **Rodar o script** no banco criado: `psql "<connection string>" -f database/script.sql` (no Neon dá para colar no *SQL Editor*).
3. **Criar o Web Service:** New → Blueprint apontando para este repositório (lê o `render.yaml`), ou New → Web Service → Docker.
4. **Preencher as variáveis** no painel do web service (*Environment*). "Conectar" o banco ao serviço **não** cria `DB_URL` sozinho; a variável precisa existir:
   - **Jeito mais simples (Render Postgres):** `DB_URL` = a **Internal Database URL** do banco, colada como está (`postgres://usuario:senha@dpg-xxxx-a/mercadoexpress`). Usuário e senha saem da própria URL.
   - **Ou JDBC:** `DB_URL` = `jdbc:postgresql://dpg-xxxx-a:5432/mercadoexpress` + `DB_USER` + `DB_PASSWORD`.
   - Opcional: `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

   A tradução de `postgres://` para JDBC é feita pelo [`ConexaoPostgresEnvironmentPostProcessor`](src/main/java/br/com/fiap/mercadoexpress/security/config/ConexaoPostgresEnvironmentPostProcessor.java) (também aceita `DATABASE_URL`). Se nenhuma URL for definida, a aplicação para logo no início com a mensagem *"Banco nao configurado no Render: defina a variavel DB_URL..."*, em vez de tentar `localhost`.
5. **Deploy.** No primeiro startup o log mostra `Administrador inicial admin@mercado.com criado.` O *health check* usa `/`, que é pública.

| Erro no log do Render | Causa | Correção |
|---|---|---|
| `Connection to localhost:5433 refused` | Versão antiga do projeto sem `DB_URL` definida | Atualize o repositório e defina `DB_URL` (passo 4) |
| `Banco nao configurado no Render` | `DB_URL`/`DATABASE_URL` ausente | Passo 4 |
| `Schema-validation: missing table [tds_sec_mvc_tb_mercado]` | Script não rodou nesse banco | Passo 2 (`database/script.sql`) |
| `password authentication failed` | Usuário/senha errados | Recopie a URL do painel do banco |
| `UnknownHostException dpg-...` | URL interna usada com banco em outra região/conta | Use a **External** URL (com `?sslmode=require`) ou a mesma região |

> O plano free do Render hiberna após ~15 min sem acesso; a primeira requisição depois disso leva uns 50 s.

<!-- [SUBSTITUIR] Print do deploy no ar (painel do Render + app aberto) -->
![Deploy no ar](docs/16-deploy-no-ar.png)

---

## Testes

```bash
./mvnw clean package
```

Roda no perfil `dev` (H2), então passa em qualquer máquina. [`FluxoDeAutenticacaoTests`](src/test/java/br/com/fiap/mercadoexpress/security/FluxoDeAutenticacaoTests.java) percorre o fluxo pelo MockMvc:

- landing pública; `/catalogo` anônimo → `/login?restrito`;
- login com e-mail inexistente → `/cadastro?origem=login&email=...` (inclusive com `+` no e-mail);
- senha errada → `/login?erro`; admin → `/admin`; cliente → `/catalogo`;
- cadastro válido grava `CLIENTE` com hash BCrypt e e-mail minúsculo; cadastro inválido devolve erro por campo;
- cliente em `/admin` → 403; POST sem CSRF → 403; logout por POST.

Além disso, o fluxo inteiro foi validado contra o **PostgreSQL real** via `docker compose`, com o CRUD conferido por `SELECT`.

---

## Integrantes

| Nome | RM |
|---|---|
| Olavo Porto Neves | 563558 |
| Pedro Henrique França | 561940 |
| Luiz Gonçalves | 564495 |

Turma **2TDSR** · Análise e Desenvolvimento de Sistemas · FIAP
