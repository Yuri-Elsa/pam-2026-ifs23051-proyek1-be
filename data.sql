-- Referensi DDL (tabel dibuat otomatis oleh Exposed SchemaUtils)

CREATE TABLE IF NOT EXISTS users (
                                     id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100)  NOT NULL,
    username   VARCHAR(50)   NOT NULL UNIQUE,
    password   VARCHAR(255)  NOT NULL,
    photo      VARCHAR(255)  NULL,
    about      TEXT          NULL,
    created_at TIMESTAMP     NOT NULL,
    updated_at TIMESTAMP     NOT NULL
    );

CREATE TABLE IF NOT EXISTS auth_tokens (
                                           id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    auth_token    TEXT  NOT NULL UNIQUE,
    refresh_token TEXT  NOT NULL UNIQUE,
    created_at    TIMESTAMP NOT NULL,
    expires_at    TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS todos (
                                     id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL DEFAULT '',
    is_done     BOOLEAN      NOT NULL DEFAULT false,
    urgency     VARCHAR(10)  NOT NULL DEFAULT 'medium',
    cover       TEXT         NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
    );

-- Nilai urgency:
--   low    → 🔵 Sedang Ditonton
--   medium → 🟣 Belum Ditonton
--   high   → 🟢 Sudah Ditonton
