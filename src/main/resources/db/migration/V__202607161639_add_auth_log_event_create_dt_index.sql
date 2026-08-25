--liquibase formatted sql
--changeset wayneyu:V__202607161639_add_auth_log_event_create_dt_index.sql splitStatements:true endDelimiter:;

CREATE INDEX IF NOT EXISTS idx_auth_log_event_create_dt ON authentication_log (event, create_dt);
