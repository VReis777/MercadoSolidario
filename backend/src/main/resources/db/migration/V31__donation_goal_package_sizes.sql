-- Ajusta as embalagens para apresentações reais do mercado brasileiro:
-- feijão/açúcar/sal/farinha em pacotes de 1 kg, café e macarrão em pacotes
-- de 500 g, óleo em garrafas de 900 ml, leite em caixas UHT de 1 L,
-- molho em sachês de 340 g. Quantidades por família permanecem.

UPDATE donation_goal_items SET package_size = 1    WHERE name = 'Feijão';
UPDATE donation_goal_items SET package_size = 1    WHERE name = 'Açúcar';
UPDATE donation_goal_items SET package_size = 0.9  WHERE name = 'Óleo de soja';
UPDATE donation_goal_items SET package_size = 0.5  WHERE name = 'Café';
UPDATE donation_goal_items SET package_size = 1    WHERE name = 'Leite';
UPDATE donation_goal_items SET package_size = 1    WHERE name = 'Farinha de trigo';
UPDATE donation_goal_items SET package_size = 0.5  WHERE name = 'Macarrão';
UPDATE donation_goal_items SET package_size = 0.34 WHERE name = 'Molho/extrato de tomate';

-- Quantidades por família alinhadas a embalagens inteiras:
-- óleo = 3 garrafas de 900 ml (2,7 L); molho = 3 sachês de 340 g (1,02 kg).
UPDATE donation_goal_items SET per_family_quantity = 2.7  WHERE name = 'Óleo de soja';
UPDATE donation_goal_items SET per_family_quantity = 1.02 WHERE name = 'Molho/extrato de tomate';
