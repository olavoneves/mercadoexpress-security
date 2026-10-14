-- Massa de teste do perfil dev (H2 em memoria).
-- As tabelas sao criadas pelo Hibernate (ddl-auto=create-drop); aqui entram so
-- os produtos. O ID fica de fora: a coluna e IDENTITY, como no PostgreSQL.
-- O administrador vem do AdminInicial (CommandLineRunner), nunca deste arquivo.

INSERT INTO TDS_SEC_MVC_TB_MERCADO (NOME, TIPO, SETOR, TAMANHO, PRECO, DESCRICAO, ESTOQUE, ATIVO, DATA_CADASTRO) VALUES
('Banana prata', 'Fruta', 'Hortifruti', '1kg', 7.99, 'Banana prata madura, colhida no Vale do Ribeira e entregue na feira duas vezes por semana.', 120, TRUE, DATE '2026-08-02'),
('Tomate italiano', 'Legume', 'Hortifruti', '1kg', 9.49, 'Tomate italiano firme, ideal para molho caseiro. Vendido a granel.', 64, TRUE, DATE '2026-08-02'),
('Alface crespa', 'Verdura', 'Hortifruti', 'Unidade', 3.90, 'Alface crespa hidropônica, embalada no mesmo dia da colheita.', 0, TRUE, DATE '2026-08-05'),
('Pão francês', 'Panificação', 'Padaria', '1kg', 18.90, 'Assado de hora em hora, das 6h às 19h. Casca crocante e miolo leve.', 40, TRUE, DATE '2026-08-06'),
('Bolo de fubá caseiro', 'Confeitaria', 'Padaria', '500g', 22.00, 'Bolo de fubá com erva-doce, receita da casa, feito todo dia de manhã.', 12, TRUE, DATE '2026-08-06'),
('Café torrado e moído', 'Mercearia seca', 'Mercearia', '500g', 21.90, 'Café 100% arábica, torra média, moagem para coador de pano.', 85, TRUE, DATE '2026-08-08'),
('Suco de laranja integral', 'Bebida', 'Bebidas', '1L', 14.50, 'Suco integral sem açúcar, prensado na hora e resfriado. Validade de três dias.', 30, TRUE, DATE '2026-08-10'),
('Água mineral com gás', 'Bebida', 'Bebidas', '500ml', 3.25, 'Água mineral naturalmente gaseificada, garrafa retornável.', 200, TRUE, DATE '2026-08-10'),
('Detergente neutro', 'Limpeza', 'Higiene e Limpeza', '500ml', 3.49, 'Detergente neutro biodegradável, rende até 40% mais que o comum.', 150, TRUE, DATE '2026-08-12'),
('Sabão em pó', 'Limpeza', 'Higiene e Limpeza', '1,6kg', 24.90, 'Sabão em pó concentrado para máquina e lavagem à mão.', 48, TRUE, DATE '2026-08-12'),
('Muçarela fatiada', 'Frios', 'Frios e Laticínios', '200g', 16.80, 'Muçarela fatiada na hora, no ponto certo para lanche e pizza.', 26, TRUE, DATE '2026-08-14'),
('Meia esportiva algodão', 'Vestuário', 'Bazar', 'M', 19.90, 'Par de meias esportivas de algodão com punho reforçado.', 18, TRUE, DATE '2026-08-15'),
('Panetone tradicional', 'Doce', 'Mercearia', '400g', 27.90, 'Item sazonal de fim de ano, mantido fora do catálogo no resto do ano.', 0, FALSE, DATE '2026-08-16');
