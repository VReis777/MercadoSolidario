-- Aluguel da família e regras v2: renda per capita efetiva (renda - aluguel)
-- e exclusão (fim da fila) para renda alta sem aluguel.

ALTER TABLE families ADD COLUMN IF NOT EXISTS pays_rent BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE families ADD COLUMN IF NOT EXISTS rent_value NUMERIC(12, 2);

-- Apenas uma configuração pode ficar ativa: desliga a v1 antes de ativar a v2.
UPDATE priority_configurations SET active = false WHERE active = true;

-- Nova versão das regras fixas (v1 fica preservada, mas desligada).
INSERT INTO priority_configurations (code, name, version_number, explanation, active, created_by, updated_by, created_at, updated_at)
SELECT 'REGRAS_FIXAS_2026', 'Regras fixas — Natal com Dignidade 2026', 2,
       'Prioridade definida pela coordenação: 1) renda por pessoa da família DESCONTANDO o aluguel ' ||
       '(quanto menor, maior a prioridade); 2) tamanho do grupo familiar; 3) fatores de vulnerabilidade ' ||
       '(desemprego, doença, moradia, dependentes). Renda acima de R$ 2.100 sem aluguel vai para o fim ' ||
       'da lista e exige avaliação da equipe antes de qualquer contemplação. ' ||
       'O resultado é técnico e apoia, não substitui, a decisão da equipe.',
       true, 'sistema', 'sistema', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM priority_configurations WHERE code = 'REGRAS_FIXAS_2026' AND version_number = 2);

INSERT INTO priority_rules (configuration_id, factor_code, label, operator, reference_value, weight, enabled, explanation)
SELECT c.id, x.factor_code, x.label, x.operator, x.reference_value, x.weight, true, x.explanation
FROM priority_configurations c
JOIN (VALUES
    ('EFFECTIVE_PER_CAPITA_INCOME', 'Renda por pessoa (menos aluguel) abaixo de R$ 200', 'LT', '200', 60,
     'Situação crítica: menos de R$ 200 por pessoa no mês, já descontando o aluguel.'),
    ('EFFECTIVE_PER_CAPITA_INCOME', 'Renda por pessoa (menos aluguel) abaixo de 1/4 do salário mínimo', 'LT', '405', 35,
     'Renda por pessoa menor que 1/4 do salário mínimo, já descontando o aluguel.'),
    ('EFFECTIVE_PER_CAPITA_INCOME', 'Renda por pessoa (menos aluguel) abaixo de 1/2 salário mínimo', 'LT', '810', 15,
     'Renda por pessoa menor que meio salário mínimo, já descontando o aluguel.'),
    ('HOUSEHOLD_SIZE', 'Família com 4 ou mais pessoas', 'GTE', '4', 20,
     'Quanto mais gente na casa, maior a prioridade — sempre depois da renda por pessoa.'),
    ('HOUSEHOLD_SIZE', 'Família numerosa (6 ou mais pessoas)', 'GTE', '6', 10,
     'Agravo adicional para famílias grandes.'),
    ('DEPENDENTS', '3 ou mais dependentes', 'GTE', '3', 10,
     'Muitas crianças/dependentes aumentam a prioridade.'),
    ('REQUEST_REASON', 'Desemprego', 'EQ', 'DESEMPREGO', 15,
     'Responsável desempregado — ex.: mãe solo sem renda.'),
    ('REQUEST_REASON', 'Doença na família', 'EQ', 'DOENCA_FAMILIA', 10,
     'Doença de familiar que compromete a renda.'),
    ('REQUEST_REASON', 'Situação de moradia precária', 'EQ', 'SITUACAO_HABITACIONAL', 10,
     'Moradia irregular ou em risco.'),
    ('NO_RENT_INCOME', 'Renda acima de R$ 2.100 sem pagar aluguel', 'GT', '2100', -1000,
     'Família com renda acima de R$ 2.100 que NÃO paga aluguel: vai para o fim da lista e a ' ||
     'equipe (Pastor/RH/Tesouraria) avalia se permanece como beneficiária.')
) AS x(factor_code, label, operator, reference_value, weight, explanation) ON true
WHERE c.code = 'REGRAS_FIXAS_2026' AND c.version_number = 2
  AND NOT EXISTS (
      SELECT 1 FROM priority_rules r
      WHERE r.configuration_id = c.id AND r.factor_code = x.factor_code AND r.reference_value = x.reference_value
  );
