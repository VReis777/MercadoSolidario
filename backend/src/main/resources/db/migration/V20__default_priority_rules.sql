-- Regras fixas de prioridade do Mercado Solidário (Natal com Dignidade).
-- Ordem conceitual: renda por pessoa da família (per capita) pesa mais,
-- depois o tamanho do grupo familiar, depois fatores de vulnerabilidade.
-- Idempotente: só insere se ainda não existir configuração com este código.

INSERT INTO priority_configurations (code, name, version_number, explanation, active, created_by, updated_by, created_at, updated_at)
SELECT 'REGRAS_FIXAS_2026', 'Regras fixas — Natal com Dignidade 2026', 1,
       'Prioridade definida pela coordenação: 1) renda por pessoa da família (quanto menor, maior a prioridade); ' ||
       '2) tamanho do grupo familiar; 3) fatores de vulnerabilidade (desemprego, doença, moradia, dependentes). ' ||
       'O resultado é técnico e apoia, não substitui, a decisão da equipe.',
       true, 'sistema', 'sistema', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM priority_configurations WHERE code = 'REGRAS_FIXAS_2026');

-- Apenas uma configuração pode ficar ativa: as anteriores são preservadas, mas desligadas.
UPDATE priority_configurations SET active = false
WHERE active = true AND code <> 'REGRAS_FIXAS_2026';

INSERT INTO priority_rules (configuration_id, factor_code, label, operator, reference_value, weight, enabled, explanation)
SELECT c.id, x.factor_code, x.label, x.operator, x.reference_value, x.weight, true, x.explanation
FROM priority_configurations c
JOIN (VALUES
    ('PER_CAPITA_INCOME', 'Renda por pessoa abaixo de R$ 200', 'LT', '200', 60,
     'Situação crítica: menos de R$ 200 por pessoa no mês.'),
    ('PER_CAPITA_INCOME', 'Renda por pessoa abaixo de 1/4 do salário mínimo', 'LT', '405', 35,
     'Renda por pessoa menor que 1/4 do salário mínimo (família com 4+ pessoas e até 1 salário).'),
    ('PER_CAPITA_INCOME', 'Renda por pessoa abaixo de 1/2 salário mínimo', 'LT', '810', 15,
     'Renda por pessoa menor que meio salário mínimo.'),
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
     'Moradia irregular ou em risco.')
) AS x(factor_code, label, operator, reference_value, weight, explanation) ON true
WHERE c.code = 'REGRAS_FIXAS_2026' AND c.version_number = 1
  AND NOT EXISTS (
      SELECT 1 FROM priority_rules r
      WHERE r.configuration_id = c.id AND r.factor_code = x.factor_code AND r.reference_value = x.reference_value
  );
