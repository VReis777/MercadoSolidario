-- Regras v3: régua de renda efetiva per capita em 350 (acima desce, abaixo sobe),
-- marca de revisão acima de 420, prioridade para família monoparental,
-- e exclusão (fim da fila) para renda alta sem aluguel.

-- Apenas uma configuração pode ficar ativa.
UPDATE priority_configurations SET active = false WHERE active = true;

INSERT INTO priority_configurations (code, name, version_number, explanation, active, created_by, updated_by, created_at, updated_at)
SELECT 'REGRAS_FIXAS_2026', 'Regras fixas — Natal com Dignidade 2026', 3,
       'Régua: renda efetiva por pessoa (renda menos aluguel) abaixo de R$ 350 sobe na lista; ' ||
       'acima de R$ 420 recebe marca de revisão e desce. Família monoparental (sem cônjuge, com ' ||
       'dependentes) ganha prioridade, com 2+ crianças acima de 1 criança. Tamanho da família, ' ||
       'dependentes e motivo da solicitação somam pontos extras. Renda acima de R$ 2.100 sem ' ||
       'aluguel vai para o fim da lista e exige avaliação da equipe.',
       true, 'sistema', 'sistema', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM priority_configurations WHERE code = 'REGRAS_FIXAS_2026' AND version_number = 3);

INSERT INTO priority_rules (configuration_id, factor_code, label, operator, reference_value, weight, enabled, explanation)
SELECT c.id, x.factor_code, x.label, x.operator, x.reference_value, x.weight, true, x.explanation
FROM priority_configurations c
JOIN (VALUES
    ('EFFECTIVE_PER_CAPITA_INCOME', 'Renda por pessoa (menos aluguel) abaixo de R$ 200', 'LT', '200', 60,
     'Situação crítica: menos de R$ 200 por pessoa no mês, já descontando o aluguel.'),
    ('EFFECTIVE_PER_CAPITA_INCOME', 'Renda por pessoa (menos aluguel) abaixo de R$ 350', 'LT', '350', 40,
     'Abaixo da régua de R$ 350 por pessoa: sobe na lista de prioridade.'),
    ('SINGLE_PARENT_DEPENDENTS', 'Pai ou mãe solo com 2 ou mais dependentes', 'GTE', '2', 40,
     'Família monoparental com 2+ crianças/dependentes — maior prioridade entre os monoparentais.'),
    ('SINGLE_PARENT_DEPENDENTS', 'Pai ou mãe solo com 1 dependente', 'GTE', '1', 20,
     'Família monoparental com criança/dependente — prioridade sobre casais.'),
    ('EFFECTIVE_PER_CAPITA_INCOME', 'Renda por pessoa (menos aluguel) acima de R$ 420', 'GT', '420', -150,
     'Acima da régua: recebe marca de revisão (*) e desce na lista para avaliação da equipe.'),
    ('HOUSEHOLD_SIZE', 'Família com 4 ou mais pessoas', 'GTE', '4', 20,
     'Quanto mais gente na casa, maior a prioridade — depois da renda por pessoa.'),
    ('HOUSEHOLD_SIZE', 'Família numerosa (6 ou mais pessoas)', 'GTE', '6', 10,
     'Agravo adicional para famílias grandes.'),
    ('DEPENDENTS', '3 ou mais dependentes', 'GTE', '3', 10,
     'Muitas crianças/dependentes aumentam a prioridade.'),
    ('REQUEST_REASON', 'Desemprego', 'EQ', 'DESEMPREGO', 15,
     'Responsável desempregado — inclui casal em que um dos dois está sem trabalhar.'),
    ('REQUEST_REASON', 'Doença na família', 'EQ', 'DOENCA_FAMILIA', 10,
     'Doença de familiar que compromete a renda.'),
    ('REQUEST_REASON', 'Situação de moradia precária', 'EQ', 'SITUACAO_HABITACIONAL', 10,
     'Moradia irregular ou em risco.'),
    ('NO_RENT_INCOME', 'Renda acima de R$ 2.100 sem pagar aluguel', 'GT', '2100', -1000,
     'Família com renda acima de R$ 2.100 que NÃO paga aluguel: vai para o fim da lista e a ' ||
     'equipe (Pastor/RH/Tesouraria) avalia se permanece como beneficiária.')
) AS x(factor_code, label, operator, reference_value, weight, explanation) ON true
WHERE c.code = 'REGRAS_FIXAS_2026' AND c.version_number = 3
  AND NOT EXISTS (
      SELECT 1 FROM priority_rules r
      WHERE r.configuration_id = c.id AND r.factor_code = x.factor_code AND r.reference_value = x.reference_value
  );
