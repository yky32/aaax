-- Fixed smoke accounts for AAAX login quality gate (Testcontainers / local / staging).
-- Passwords (BCrypt):
--   smoke.primary@aaax.local   -> SmokePrimary!1
--   smoke.secondary@aaax.local -> SmokeSecondary!2
-- Idempotent: safe to re-run.

CREATE TABLE IF NOT EXISTS users (
    id              bigint PRIMARY KEY,
    username        varchar(255) UNIQUE,
    status          varchar(32),
    metadata        jsonb,
    source_system_tags jsonb,
    is_active       boolean NOT NULL DEFAULT true,
    created_by      varchar(255),
    created_date    timestamp,
    last_modified_by varchar(255),
    last_modified_date timestamp
);

CREATE TABLE IF NOT EXISTS authentications (
    id              bigint PRIMARY KEY,
    user_id         bigint REFERENCES users (id),
    identifier      varchar(255),
    login_type      varchar(32),
    credentials     text,
    last_login_dt   timestamp,
    attempts        integer,
    credentials_histories jsonb,
    is_active       boolean NOT NULL DEFAULT true,
    created_by      varchar(255),
    created_date    timestamp,
    last_modified_by varchar(255),
    last_modified_date timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_auth_user_login_identifier
    ON authentications (user_id, login_type, identifier);

-- Primary
INSERT INTO users (id, username, status, is_active, created_by, created_date)
VALUES (9100000000000000001, 'smoke.primary@aaax.local', 'ACTIVE', true, 'login-smoke', NOW())
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    status = 'ACTIVE',
    is_active = true;

INSERT INTO authentications (id, user_id, identifier, login_type, credentials, attempts, is_active, created_by, created_date)
VALUES (
    9100000000000000011,
    9100000000000000001,
    'smoke.primary@aaax.local',
    'EMAIL',
    '$2b$10$Vca0QR/ugbY8aO14H.XzmOOj3crrzIRGB10d2v1m9Mtg0o5YoYJ9m',
    0,
    true,
    'login-smoke',
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    credentials = EXCLUDED.credentials,
    is_active = true,
    attempts = 0;

-- Secondary
INSERT INTO users (id, username, status, is_active, created_by, created_date)
VALUES (9100000000000000002, 'smoke.secondary@aaax.local', 'ACTIVE', true, 'login-smoke', NOW())
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    status = 'ACTIVE',
    is_active = true;

INSERT INTO authentications (id, user_id, identifier, login_type, credentials, attempts, is_active, created_by, created_date)
VALUES (
    9100000000000000012,
    9100000000000000002,
    'smoke.secondary@aaax.local',
    'EMAIL',
    '$2b$10$0epEl1wWEPusvHbnI9SU4OSDGxj9z5W0/kv/mtKtH15xYQrj.thha',
    0,
    true,
    'login-smoke',
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    credentials = EXCLUDED.credentials,
    is_active = true,
    attempts = 0;
