-- Vagas iniciais por modalidade: 150 cada (ajustável depois pelo Pastor).
UPDATE system_settings
    SET setting_value = '150'
WHERE setting_key IN ('RUM_SLOTS_3KM', 'RUM_SLOTS_6KM');
