-- Regras v4: a renda TOTAL vem primeiro (quem ganha menos fica acima),
-- depois a renda efetiva por pessoa (renda - aluguel), monoparental,
-- tamanho da família e motivos. Teto: renda > 3.300 com aluguel recebe
-- marca (*) e vai para baixo; sem aluguel é exclusão (fim da lista).

UPDATE priority_configurations SET active = false WHERE active = true;

INSERT INTO priority_configurations (code, name, version_number, explanation, active, created_by, updated_by, created_at, updated_at)
SELECT 'REGRAS_FIXAS_2026', 'Regras fixas — Natal com Dignidade 2026', 4,
       'Ordem: 1) renda total da família — quanto menor, mais acima (quem ganha mais compra ' ||
       'a cesta com mais facilidade, ainda mais em dezembro com 13o); acima de 1 salário desce, ' ||
       'acima de 1,5 salário desce mais, acima de R$ 3.300 recebe marca (*) e vai para baixo — ' ||
       'sem aluguel, vai para o fim da lista e exige avaliação. 2) Renda efetiva por pessoa ' ||
       '(renda menos aluguel): abaixo de R$ 350 sobe, acima de R$ 420 recebe (*). ' ||
       '3) Pai/mãe solo com dependentes, tamanho da família e motivo da solicitação.',
       true, 'sistema', 'sistema', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM priority_configurations WHERE code = 'REGRAS_FIXAS_2026' AND version_number = 4);

INSERT INTO priority_rules (configuration_id, factor_code, label, operator, reference_value, weight, enabled, explanation)
SELECT c.id, x.factor_code, x.label, x.operator, x.reference_value, x.weight, true, x.explanation
FROM priority_configurations c
JOIN (VALUES
    ('TOTAL_INCOME', 'Renda familiar acima de 1 salário mínimo', 'GT', '1621', -300,
     'Quem ganha mais de 1 salário desce na lista — a renda total vem antes de qualquer outro critério.'),
    ('TOTAL_INCOME', 'Renda familiar acima de 1,5 salário mínimo', 'GT', '2431', -400,
     'Renda acima de 1,5 salário desce ainda mais — outras famílias têm mais dificuldade de comprar a cesta.'),
    ('TOTAL_INCOME', 'Renda familiar acima de R$ 3.300', 'GT', '3300', -150,
     'Teto de renda: só passa na fila se pagar aluguel; recebe marca (*) e vai para baixo, para avaliação do Pastor/RH.'),
    ('NO_RENT_INCOME', 'Renda acima de R$ 2.100 sem pagar aluguel', 'GT', '2100', -150,
     'Renda acima de R$ 2.100 sem aluguel recebe marca (*) — ex.: mãe/pai solo com 2 crianças ainda é avaliado.'),
    ('NO_RENT_INCOME', 'Renda acima de R$ 3.300 sem pagar aluguel', 'GT', '3300', -1000,
     'Renda acima de R$ 3.300 e sem aluguel: fim da lista, avaliação obrigatória da equipe.'),
    ('EFFECTIVE_PER_CAPITA_INCOME', 'Renda por pessoa (menos aluguel) abaixo de R$ 200', 'LT', '200', 60,
     'Situação crítica: menos de R$ 200 por pessoa no mês, já descontando o aluguel.'),
    ('EFFECTIVE_PER_CAPITA_INCOME', 'Renda por pessoa (menos aluguel) abaixo de R$ 350', 'LT', '350', 40,
     'Abaixo da régua de R$ 350 por pessoa: sobe na lista de prioridade.'),
    ('EFFECTIVE_PER_CAPITA_INCOME', 'Renda por pessoa (menos aluguel) acima de R$ 420', 'GT', '420', -150,
     'Acima da régua: recebe marca de revisão (*) e desce na lista para avaliação da equipe.'),
    ('SINGLE_PARENT_DEPENDENTS', 'Pai ou mãe solo com 2 ou mais dependentes', 'GTE', '2', 40,
     'Família monoparental com 2+ crianças/dependentes — maior prioridade entre os monoparentais.'),
    ('SINGLE_PARENT_DEPENDENTS', 'Pai ou mãe solo com 1 dependente', 'GTE', '1', 20,
     'Família monoparental com criança/dependente — prioridade sobre casais.'),
    ('HOUSEHOLD_SIZE', 'Família com 4 ou mais pessoas', 'GTE', '4', 20,
     'Quanto mais gente na casa, maior a prioridade — depois da renda.'),
    ('HOUSEHOLD_SIZE', 'Família numerosa (6 ou mais pessoas)', 'GTE', '6', 10,
     'Agravo adicional para famílias grandes.'),
    ('DEPENDENTS', '3 ou mais dependentes', 'GTE', '3', 10,
     'Muitas crianças/dependentes aumentam a prioridade.'),
    ('REQUEST_REASON', 'Desemprego', 'EQ', 'DESEMPREGO', 15,
     'Responsável desempregado — inclui casal em que um dos dois está sem trabalhar.'),
    ('REQUEST_REASON', 'Doença na família', 'EQ', 'DOENCA_FAMILIA', 10,
     'Doença de familiar que compromete a renda.'),
    ('REQUEST_REASON', 'Situação de moradia precária', 'EQ', 'SITUACAO_HABITACIONAL', 10,
     'Moradia irregular ou em risco.')
) AS x(factor_code, label, operator, reference_value, weight, explanation) ON true
WHERE c.code = 'REGRAS_FIXAS_2026' AND c.version_number = 4
  AND NOT EXISTS (
      SELECT 1 FROM priority_rules r
      WHERE r.configuration_id = c.id AND r.factor_code = x.factor_code AND r.reference_value = x.reference_value
  );
