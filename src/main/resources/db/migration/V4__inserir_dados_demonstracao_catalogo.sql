INSERT INTO unidade (nome, cidade, endereco, tipo, unidade_matriz_id, ativo) VALUES
    ('Raízes Matriz Recife', 'Recife', 'Rua da Aurora, 120, Santo Amaro', 'MATRIZ', NULL, TRUE);

INSERT INTO unidade (nome, cidade, endereco, tipo, unidade_matriz_id, ativo)
SELECT 'Raízes Filial Olinda', 'Olinda', 'Avenida Getúlio Vargas, 450', 'FILIAL', id, TRUE
FROM unidade
WHERE tipo = 'MATRIZ';

INSERT INTO produto (nome, descricao, preco, ativo) VALUES
    ('Baião de dois', 'Arroz, feijão de corda, queijo coalho e carne de sol.', 34.90, TRUE),
    ('Carne de sol com macaxeira', 'Carne de sol acebolada com macaxeira frita.', 46.00, TRUE),
    ('Tapioca de queijo coalho', 'Tapioca recheada com queijo coalho.', 14.00, TRUE);

INSERT INTO estoque (unidade_id, produto_id, quantidade_total, quantidade_reservada)
SELECT unidade.id, produto.id, dados.quantidade_total, 0
FROM (
    VALUES
        ('Raízes Matriz Recife', 'Baião de dois', 40),
        ('Raízes Matriz Recife', 'Carne de sol com macaxeira', 30),
        ('Raízes Matriz Recife', 'Tapioca de queijo coalho', 50),
        ('Raízes Filial Olinda', 'Baião de dois', 18),
        ('Raízes Filial Olinda', 'Carne de sol com macaxeira', 12),
        ('Raízes Filial Olinda', 'Tapioca de queijo coalho', 36)
) AS dados (unidade_nome, produto_nome, quantidade_total)
JOIN unidade ON unidade.nome = dados.unidade_nome
JOIN produto ON produto.nome = dados.produto_nome;
