-- =====================================================================
-- FIAP - TDS - Checkpoint 5 (Parte I - Spring Security MVC + PostgreSQL)
-- Projeto: Mercado Express Security
-- Banco...: PostgreSQL 16 (local via docker compose, Render Postgres ou Neon)
--
-- Como executar:
--   * docker compose: roda sozinho na primeira subida do container "db"
--     (montado em /docker-entrypoint-initdb.d).
--   * Render / Neon / qualquer Postgres:
--       psql "postgresql://USUARIO:SENHA@HOST:5432/BANCO?sslmode=require" -f database/script.sql
--     ou cole o conteudo no SQL Editor do Neon.
--
-- A aplicacao roda com spring.jpa.hibernate.ddl-auto=validate: quem cria as
-- tabelas e ESTE script. O Hibernate apenas confere se as entidades batem.
--
-- Usuarios NAO sao inseridos aqui: o administrador inicial e criado pela
-- aplicacao no startup (AdminInicial / CommandLineRunner), porque o hash
-- BCrypt precisa sair do mesmo PasswordEncoder usado no login.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) LIMPEZA (idempotente: pode rodar quantas vezes quiser)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS TDS_SEC_MVC_TB_MERCADO;
DROP TABLE IF EXISTS TDS_USERS_MERCADO;

-- ---------------------------------------------------------------------
-- 2) PRODUTOS - mesmas colunas do Checkpoint 4
--    No Oracle a coluna ATIVO era CHAR(1) 'S'/'N'; aqui e BOOLEAN nativo.
-- ---------------------------------------------------------------------
CREATE TABLE TDS_SEC_MVC_TB_MERCADO (
    ID             BIGINT         GENERATED ALWAYS AS IDENTITY,
    NOME           VARCHAR(100)   NOT NULL,
    TIPO           VARCHAR(50)    NOT NULL,
    SETOR          VARCHAR(50)    NOT NULL,
    TAMANHO        VARCHAR(30),
    PRECO          NUMERIC(10,2)  NOT NULL,
    DESCRICAO      VARCHAR(500),
    ESTOQUE        INTEGER        NOT NULL DEFAULT 0,
    ATIVO          BOOLEAN        NOT NULL DEFAULT TRUE,
    DATA_CADASTRO  DATE           NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT PK_TDS_SEC_MERCADO     PRIMARY KEY (ID),
    CONSTRAINT CK_TDS_SEC_PRECO       CHECK (PRECO > 0),
    CONSTRAINT CK_TDS_SEC_ESTOQUE     CHECK (ESTOQUE >= 0)
);

COMMENT ON TABLE  TDS_SEC_MVC_TB_MERCADO               IS 'Produtos do catalogo do Mercado Express (CP5 - Spring Security MVC)';
COMMENT ON COLUMN TDS_SEC_MVC_TB_MERCADO.ID            IS 'Identificador gerado pelo proprio banco (IDENTITY)';
COMMENT ON COLUMN TDS_SEC_MVC_TB_MERCADO.NOME          IS 'Nome comercial do produto';
COMMENT ON COLUMN TDS_SEC_MVC_TB_MERCADO.TIPO          IS 'Tipo do produto (Fruta, Limpeza, Bebida...)';
COMMENT ON COLUMN TDS_SEC_MVC_TB_MERCADO.SETOR         IS 'Setor do mercado (Hortifruti, Padaria, Bazar...)';
COMMENT ON COLUMN TDS_SEC_MVC_TB_MERCADO.TAMANHO       IS 'Tamanho ou volume da embalagem (1kg, 500ml, M...)';
COMMENT ON COLUMN TDS_SEC_MVC_TB_MERCADO.PRECO         IS 'Preco unitario em reais, sempre maior que zero';
COMMENT ON COLUMN TDS_SEC_MVC_TB_MERCADO.DESCRICAO     IS 'Texto exibido na ficha do produto';
COMMENT ON COLUMN TDS_SEC_MVC_TB_MERCADO.ESTOQUE       IS 'Quantidade disponivel; zero marca o card como esgotado';
COMMENT ON COLUMN TDS_SEC_MVC_TB_MERCADO.ATIVO         IS 'TRUE = aparece no catalogo, FALSE = fora de linha';
COMMENT ON COLUMN TDS_SEC_MVC_TB_MERCADO.DATA_CADASTRO IS 'Data em que o produto entrou no catalogo';

CREATE INDEX IX_TDS_SEC_SETOR ON TDS_SEC_MVC_TB_MERCADO (SETOR);
CREATE INDEX IX_TDS_SEC_NOME  ON TDS_SEC_MVC_TB_MERCADO (NOME);

-- ---------------------------------------------------------------------
-- 3) USUARIOS - perfis gravados pelo cadastro (Sign Up) e pelo startup
-- ---------------------------------------------------------------------
CREATE TABLE TDS_USERS_MERCADO (
    ID             BIGINT         GENERATED ALWAYS AS IDENTITY,
    NOME           VARCHAR(100)   NOT NULL,
    EMAIL          VARCHAR(120)   NOT NULL,
    SENHA          VARCHAR(100)   NOT NULL,
    PERFIL         VARCHAR(20)    NOT NULL DEFAULT 'CLIENTE',
    DATA_CADASTRO  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ATIVO          BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT PK_TDS_USERS            PRIMARY KEY (ID),
    CONSTRAINT UK_TDS_USERS_EMAIL      UNIQUE (EMAIL),
    -- A aplicacao grava o e-mail em minusculas; o banco garante isso tambem.
    CONSTRAINT CK_TDS_USERS_EMAIL_MIN  CHECK (EMAIL = LOWER(EMAIL)),
    CONSTRAINT CK_TDS_USERS_PERFIL     CHECK (PERFIL IN ('CLIENTE', 'ADMIN'))
);

COMMENT ON TABLE  TDS_USERS_MERCADO               IS 'Contas de acesso do Mercado Express (Spring Security)';
COMMENT ON COLUMN TDS_USERS_MERCADO.EMAIL         IS 'Login do usuario (username do Spring Security), unico e minusculo';
COMMENT ON COLUMN TDS_USERS_MERCADO.SENHA         IS 'Hash BCrypt da senha - nunca texto puro';
COMMENT ON COLUMN TDS_USERS_MERCADO.PERFIL        IS 'CLIENTE = catalogo e minha conta | ADMIN = tambem o painel /admin';
COMMENT ON COLUMN TDS_USERS_MERCADO.ATIVO         IS 'FALSE impede o login (DisabledException)';

-- ---------------------------------------------------------------------
-- 4) MASSA DE TESTE - variedade real de mercado de bairro (13 produtos)
--    O ID nao e informado: GENERATED ALWAYS AS IDENTITY numera sozinho.
-- ---------------------------------------------------------------------
INSERT INTO TDS_SEC_MVC_TB_MERCADO (NOME, TIPO, SETOR, TAMANHO, PRECO, DESCRICAO, ESTOQUE, ATIVO, DATA_CADASTRO) VALUES
('Banana prata', 'Fruta', 'Hortifruti', '1kg', 7.99,
 'Banana prata madura, colhida no Vale do Ribeira e entregue na feira duas vezes por semana.', 120, TRUE, DATE '2026-08-02'),
('Tomate italiano', 'Legume', 'Hortifruti', '1kg', 9.49,
 'Tomate italiano firme, ideal para molho caseiro. Vendido a granel.', 64, TRUE, DATE '2026-08-02'),
('Alface crespa', 'Verdura', 'Hortifruti', 'Unidade', 3.90,
 'Alface crespa hidropônica, embalada no mesmo dia da colheita.', 0, TRUE, DATE '2026-08-05'),
('Pão francês', 'Panificação', 'Padaria', '1kg', 18.90,
 'Assado de hora em hora, das 6h às 19h. Casca crocante e miolo leve.', 40, TRUE, DATE '2026-08-06'),
('Bolo de fubá caseiro', 'Confeitaria', 'Padaria', '500g', 22.00,
 'Bolo de fubá com erva-doce, receita da casa, feito todo dia de manhã.', 12, TRUE, DATE '2026-08-06'),
('Café torrado e moído', 'Mercearia seca', 'Mercearia', '500g', 21.90,
 'Café 100% arábica, torra média, moagem para coador de pano.', 85, TRUE, DATE '2026-08-08'),
('Suco de laranja integral', 'Bebida', 'Bebidas', '1L', 14.50,
 'Suco integral sem açúcar, prensado na hora e resfriado. Validade de três dias.', 30, TRUE, DATE '2026-08-10'),
('Água mineral com gás', 'Bebida', 'Bebidas', '500ml', 3.25,
 'Água mineral naturalmente gaseificada, garrafa retornável.', 200, TRUE, DATE '2026-08-10'),
('Detergente neutro', 'Limpeza', 'Higiene e Limpeza', '500ml', 3.49,
 'Detergente neutro biodegradável, rende até 40% mais que o comum.', 150, TRUE, DATE '2026-08-12'),
('Sabão em pó', 'Limpeza', 'Higiene e Limpeza', '1,6kg', 24.90,
 'Sabão em pó concentrado para máquina e lavagem à mão.', 48, TRUE, DATE '2026-08-12'),
('Muçarela fatiada', 'Frios', 'Frios e Laticínios', '200g', 16.80,
 'Muçarela fatiada na hora, no ponto certo para lanche e pizza.', 26, TRUE, DATE '2026-08-14'),
('Meia esportiva algodão', 'Vestuário', 'Bazar', 'M', 19.90,
 'Par de meias esportivas de algodão com punho reforçado.', 18, TRUE, DATE '2026-08-15'),
-- Inativo de proposito: mostra a diferenca entre o painel (ve tudo)
-- e o catalogo do cliente (so ve o que esta ativo).
('Panetone tradicional', 'Doce', 'Mercearia', '400g', 27.90,
 'Item sazonal de fim de ano, mantido fora do catálogo no resto do ano.', 0, FALSE, DATE '2026-08-16');

-- ---------------------------------------------------------------------
-- 5) CONFERENCIA
-- ---------------------------------------------------------------------
SELECT ID, NOME, SETOR, PRECO, ESTOQUE, ATIVO, DATA_CADASTRO
  FROM TDS_SEC_MVC_TB_MERCADO
 ORDER BY ID;

SELECT SETOR, COUNT(*) AS QTD_PRODUTOS
  FROM TDS_SEC_MVC_TB_MERCADO
 GROUP BY SETOR
 ORDER BY SETOR;

-- Depois do primeiro startup da aplicacao, o admin aparece aqui:
-- SELECT ID, NOME, EMAIL, PERFIL, ATIVO, DATA_CADASTRO FROM TDS_USERS_MERCADO ORDER BY ID;
