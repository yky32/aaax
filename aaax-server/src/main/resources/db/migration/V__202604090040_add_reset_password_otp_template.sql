--liquibase formatted sql
--changeset wayneyu:V__202604090040_add_reset_password_otp_template.sql splitStatements:true endDelimiter:;

INSERT INTO public.system_configuration (id, create_dt, created_by, update_dt, updated_by, version, is_active, name, scope, target, value) VALUES (7219025547286926533, '2024-07-16 17:10:13.482026 +00:00', '7159127341975732224', '2024-07-16 17:10:13.482026 +00:00', '7159127341975732224', 0, true, 'Reset-OTP-Tempalte-ID', 'GLOBAL', 'OTP_RESET_PASSWORD_TEMPLATE', '"d-8753872332eb45fd954cfd00a12793de"');
