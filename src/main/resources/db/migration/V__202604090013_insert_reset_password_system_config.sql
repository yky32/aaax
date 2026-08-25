--liquibase formatted sql
--changeset wayneyu:V__202604090013_insert_reset_password_system_config.sql splitStatements:true endDelimiter:;

INSERT INTO public.system_configuration (id, create_dt, created_by, update_dt, updated_by, version, is_active, name, scope, target, value) VALUES (7219025547286806529, '2024-07-16 17:10:13.482000 +00:00', 'system', '2024-07-16 17:10:13.482000 +00:00', 'system', 0, true, 'OTP-Time', 'GLOBAL', 'RESET_PASSWORD_OTP', '60');