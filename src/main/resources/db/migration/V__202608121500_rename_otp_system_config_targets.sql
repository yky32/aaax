--liquibase formatted sql
--changeset wayneyu:V__202608121500_rename_otp_system_config_targets.sql splitStatements:true endDelimiter:;

-- Clearer OTP system_configuration.target names (values stay seconds).
-- OTP                → OTP_TTL
-- OTP_INTERVAL       → OTP_RESEND_TTL
-- RESET_PASSWORD_OTP → OTP_RESET_PASSWORD_TTL
-- (also accepts intermediate names from earlier PR iterations)

UPDATE public.system_configuration
SET target = 'OTP_TTL',
    name = 'OTP TTL (seconds) — code valid for verify'
WHERE target IN ('OTP', 'OTP_LIVE', 'OTP_LIVE_TTL')
  AND scope = 'GLOBAL';

UPDATE public.system_configuration
SET target = 'OTP_RESEND_TTL',
    name = 'OTP resend TTL (seconds) — min gap between sends'
WHERE target IN ('OTP_INTERVAL', 'OTP_RESEND_INTERVAL', 'OTP_RESEND_INTERVAL_TTL')
  AND scope = 'GLOBAL';

UPDATE public.system_configuration
SET target = 'OTP_RESET_PASSWORD_TTL',
    name = 'OTP reset-password TTL (seconds)'
WHERE target IN (
    'RESET_PASSWORD_OTP',
    'RESET_PASSWORD_OTP_LIVE',
    'RESET_PASSWORD_OTP_LIVE_TTL',
    'RESET_PASSWORD_OTP_TTL'
  )
  AND scope = 'GLOBAL';
