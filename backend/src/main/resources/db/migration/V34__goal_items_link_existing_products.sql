-- Correção da V33: os 10 itens da meta já existiam no estoque
-- (ARROZ, FEIJAO, CAFÉ, ÓLEO, LEITE LONGA VIDA, MOLHO DE TOMATE...).
-- Religa cada item da meta ao produto EXISTENTE, preenche a
-- quantidade por família (cesta) nesses produtos e remove os
-- produtos duplicados criados pela V33.

-- 1) Libera doações que porventura apontem para os produtos duplicados
UPDATE donations SET product_id = NULL
WHERE product_id IN (SELECT id FROM products WHERE category = 'Cesta 2026' AND unit = 'pacote');

-- 2) Quantidade por família (cesta) nos produtos já existentes
UPDATE products SET basket_quantity = 1 WHERE UPPER(name) LIKE 'ARROZ%' AND basket_quantity IS NULL;
UPDATE products SET basket_quantity = 4 WHERE UPPER(name) LIKE 'FEIJ_O%' AND basket_quantity IS NULL;
UPDATE products SET basket_quantity = 4 WHERE UPPER(name) LIKE 'A_UCAR%' AND basket_quantity IS NULL;
UPDATE products SET basket_quantity = 1 WHERE UPPER(name) = 'SAL' AND basket_quantity IS NULL;
UPDATE products SET basket_quantity = 3 WHERE UPPER(name) LIKE '_LEO%' AND basket_quantity IS NULL;
UPDATE products SET basket_quantity = 2 WHERE UPPER(name) LIKE 'CAF_%' AND basket_quantity IS NULL;
UPDATE products SET basket_quantity = 4 WHERE UPPER(name) LIKE 'LEITE LONGA%' AND basket_quantity IS NULL;
UPDATE products SET basket_quantity = 2 WHERE UPPER(name) LIKE 'FARINHA DE TRIGO%' AND basket_quantity IS NULL;
UPDATE products SET basket_quantity = 4 WHERE UPPER(name) LIKE 'MACARR_O%' AND basket_quantity IS NULL;
UPDATE products SET basket_quantity = 3 WHERE UPPER(name) LIKE 'MOLHO%' AND basket_quantity IS NULL;

-- 3) Religa itens da meta aos produtos existentes
UPDATE donation_goal_items SET product_id = (SELECT id FROM products WHERE UPPER(name) LIKE 'ARROZ%' AND category IS DISTINCT FROM 'Cesta 2026' ORDER BY id LIMIT 1) WHERE name = 'Arroz';
UPDATE donation_goal_items SET product_id = (SELECT id FROM products WHERE UPPER(name) LIKE 'FEIJ_O%' AND category IS DISTINCT FROM 'Cesta 2026' ORDER BY id LIMIT 1) WHERE name = 'Feijão';
UPDATE donation_goal_items SET product_id = (SELECT id FROM products WHERE UPPER(name) LIKE 'A_UCAR%' AND category IS DISTINCT FROM 'Cesta 2026' ORDER BY id LIMIT 1) WHERE name = 'Açúcar';
UPDATE donation_goal_items SET product_id = (SELECT id FROM products WHERE UPPER(name) = 'SAL' AND category IS DISTINCT FROM 'Cesta 2026' ORDER BY id LIMIT 1) WHERE name = 'Sal';
UPDATE donation_goal_items SET product_id = (SELECT id FROM products WHERE UPPER(name) LIKE '_LEO%' AND category IS DISTINCT FROM 'Cesta 2026' ORDER BY id LIMIT 1) WHERE name = 'Óleo de soja';
UPDATE donation_goal_items SET product_id = (SELECT id FROM products WHERE UPPER(name) LIKE 'CAF_%' AND category IS DISTINCT FROM 'Cesta 2026' ORDER BY id LIMIT 1) WHERE name = 'Café';
UPDATE donation_goal_items SET product_id = (SELECT id FROM products WHERE UPPER(name) LIKE 'LEITE LONGA%' AND category IS DISTINCT FROM 'Cesta 2026' ORDER BY id LIMIT 1) WHERE name = 'Leite';
UPDATE donation_goal_items SET product_id = (SELECT id FROM products WHERE UPPER(name) LIKE 'FARINHA DE TRIGO%' AND category IS DISTINCT FROM 'Cesta 2026' ORDER BY id LIMIT 1) WHERE name = 'Farinha de trigo';
UPDATE donation_goal_items SET product_id = (SELECT id FROM products WHERE UPPER(name) LIKE 'MACARR_O%' AND category IS DISTINCT FROM 'Cesta 2026' ORDER BY id LIMIT 1) WHERE name = 'Macarrão';
UPDATE donation_goal_items SET product_id = (SELECT id FROM products WHERE UPPER(name) LIKE 'MOLHO%' AND category IS DISTINCT FROM 'Cesta 2026' ORDER BY id LIMIT 1) WHERE name = 'Molho/extrato de tomate';

-- 4) Remove os duplicados criados pela V33
DELETE FROM products WHERE category = 'Cesta 2026' AND unit = 'pacote';
