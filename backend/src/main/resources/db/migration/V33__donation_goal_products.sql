-- Cria no estoque os 10 produtos correspondentes aos itens da meta 2026,
-- contados em PACOTES (unidade 'pacote'), já com basket_quantity preenchido
-- (pacotes por família na cesta) e liga cada item da meta ao seu produto.
-- Idempotente: só insere produto que ainda não exista pelo nome.

INSERT INTO products (name, category, unit, current_quantity, minimum_quantity, basket_quantity, active)
SELECT 'Arroz — pacote 5 kg', 'Cesta 2026', 'pacote', 0, 0, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Arroz — pacote 5 kg');
INSERT INTO products (name, category, unit, current_quantity, minimum_quantity, basket_quantity, active)
SELECT 'Feijão — pacote 1 kg', 'Cesta 2026', 'pacote', 0, 0, 4, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Feijão — pacote 1 kg');
INSERT INTO products (name, category, unit, current_quantity, minimum_quantity, basket_quantity, active)
SELECT 'Açúcar — pacote 1 kg', 'Cesta 2026', 'pacote', 0, 0, 4, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Açúcar — pacote 1 kg');
INSERT INTO products (name, category, unit, current_quantity, minimum_quantity, basket_quantity, active)
SELECT 'Sal — pacote 1 kg', 'Cesta 2026', 'pacote', 0, 0, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Sal — pacote 1 kg');
INSERT INTO products (name, category, unit, current_quantity, minimum_quantity, basket_quantity, active)
SELECT 'Óleo de soja — garrafa 900 ml', 'Cesta 2026', 'pacote', 0, 0, 3, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Óleo de soja — garrafa 900 ml');
INSERT INTO products (name, category, unit, current_quantity, minimum_quantity, basket_quantity, active)
SELECT 'Café — pacote 500 g', 'Cesta 2026', 'pacote', 0, 0, 2, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Café — pacote 500 g');
INSERT INTO products (name, category, unit, current_quantity, minimum_quantity, basket_quantity, active)
SELECT 'Leite — caixa 1 L', 'Cesta 2026', 'pacote', 0, 0, 4, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Leite — caixa 1 L');
INSERT INTO products (name, category, unit, current_quantity, minimum_quantity, basket_quantity, active)
SELECT 'Farinha de trigo — pacote 1 kg', 'Cesta 2026', 'pacote', 0, 0, 2, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Farinha de trigo — pacote 1 kg');
INSERT INTO products (name, category, unit, current_quantity, minimum_quantity, basket_quantity, active)
SELECT 'Macarrão — pacote 500 g', 'Cesta 2026', 'pacote', 0, 0, 4, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Macarrão — pacote 500 g');
INSERT INTO products (name, category, unit, current_quantity, minimum_quantity, basket_quantity, active)
SELECT 'Molho de tomate — sachê 340 g', 'Cesta 2026', 'pacote', 0, 0, 3, TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Molho de tomate — sachê 340 g');

UPDATE donation_goal_items d SET product_id =
    (SELECT p.id FROM products p WHERE p.name = 'Arroz — pacote 5 kg')
WHERE d.name = 'Arroz';
UPDATE donation_goal_items d SET product_id =
    (SELECT p.id FROM products p WHERE p.name = 'Feijão — pacote 1 kg')
WHERE d.name = 'Feijão';
UPDATE donation_goal_items d SET product_id =
    (SELECT p.id FROM products p WHERE p.name = 'Açúcar — pacote 1 kg')
WHERE d.name = 'Açúcar';
UPDATE donation_goal_items d SET product_id =
    (SELECT p.id FROM products p WHERE p.name = 'Sal — pacote 1 kg')
WHERE d.name = 'Sal';
UPDATE donation_goal_items d SET product_id =
    (SELECT p.id FROM products p WHERE p.name = 'Óleo de soja — garrafa 900 ml')
WHERE d.name = 'Óleo de soja';
UPDATE donation_goal_items d SET product_id =
    (SELECT p.id FROM products p WHERE p.name = 'Café — pacote 500 g')
WHERE d.name = 'Café';
UPDATE donation_goal_items d SET product_id =
    (SELECT p.id FROM products p WHERE p.name = 'Leite — caixa 1 L')
WHERE d.name = 'Leite';
UPDATE donation_goal_items d SET product_id =
    (SELECT p.id FROM products p WHERE p.name = 'Farinha de trigo — pacote 1 kg')
WHERE d.name = 'Farinha de trigo';
UPDATE donation_goal_items d SET product_id =
    (SELECT p.id FROM products p WHERE p.name = 'Macarrão — pacote 500 g')
WHERE d.name = 'Macarrão';
UPDATE donation_goal_items d SET product_id =
    (SELECT p.id FROM products p WHERE p.name = 'Molho de tomate — sachê 340 g')
WHERE d.name = 'Molho/extrato de tomate';
