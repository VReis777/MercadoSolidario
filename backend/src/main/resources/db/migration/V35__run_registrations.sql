-- Inscrições da corrida Mercado Solidário Rum (3KM e 6KM).
-- Pagamento confirmado pelo gateway (PIX ou cartão) marca status=PAGA e
-- gera uma Donation FINANCEIRA/PAGA que entra na contagem de arrecadação.

CREATE TABLE run_registrations (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(60),
    modality VARCHAR(10) NOT NULL,          -- '3KM' | '6KM'
    shirt_size VARCHAR(10),                 -- P, M, G, GG, XG
    amount NUMERIC(12,2) NOT NULL,
    extra_donation NUMERIC(12,2),           -- upsell: doação extra opcional
    payment_method VARCHAR(10) NOT NULL,    -- 'PIX' | 'CARTAO'
    payment_ref VARCHAR(80),                -- id do pagamento no gateway
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Configurações editáveis pelo admin em Configurações
INSERT INTO system_settings (setting_key, setting_value, description)
SELECT 'RUM_PRICE_CENTS', '10000', 'Valor da inscrição da corrida Rum em centavos (R$100,00)'
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'RUM_PRICE_CENTS');
INSERT INTO system_settings (setting_key, setting_value, description)
SELECT 'RUM_SLOTS_3KM', '100', 'Vagas da modalidade 3KM da corrida Rum'
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'RUM_SLOTS_3KM');
INSERT INTO system_settings (setting_key, setting_value, description)
SELECT 'RUM_SLOTS_6KM', '100', 'Vagas da modalidade 6KM da corrida Rum'
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'RUM_SLOTS_6KM');
