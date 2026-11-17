-- Corrida aprovada com 7 km (antes 6 km). Renomeia a modalidade
-- preservando inscrições e vagas já registradas.

UPDATE run_registrations
SET modality = '7KM'
WHERE modality = '6KM';

UPDATE system_settings
SET setting_key = 'RUM_SLOTS_7KM',
    description = 'Vagas da modalidade 7KM da corrida Rum'
WHERE setting_key = 'RUM_SLOTS_6KM'
  AND NOT EXISTS (SELECT 1 FROM system_settings s2 WHERE s2.setting_key = 'RUM_SLOTS_7KM');

DELETE FROM system_settings WHERE setting_key = 'RUM_SLOTS_6KM';
