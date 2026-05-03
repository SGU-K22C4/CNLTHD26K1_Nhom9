-- V1__init_user_auth.sql
-- Khởi tạo schema cho user-service

CREATE TABLE IF NOT EXISTS users (
    id                   VARCHAR(36)  NOT NULL PRIMARY KEY,
    email                VARCHAR(100) NOT NULL UNIQUE,
    password             VARCHAR(255) NOT NULL,
    full_name            VARCHAR(100),
    phone                VARCHAR(20),
    avatar               VARCHAR(500),
    gender               TINYINT(1),
    role                 ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    is_email_verified    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at           DATETIME     NOT NULL,
    updated_at           DATETIME
);

CREATE TABLE IF NOT EXISTS addresses (
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id      VARCHAR(36)  NOT NULL,
    full_name    VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20)  NOT NULL,
    street       VARCHAR(255) NOT NULL,
    ward         VARCHAR(100) NOT NULL,
    city         VARCHAR(100) NOT NULL,
    is_default   TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id    VARCHAR(36)  NOT NULL,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at DATETIME     NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked    TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id    VARCHAR(36)  NOT NULL,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME     NOT NULL,
    used       TINYINT(1)   NOT NULL DEFAULT 0
);

-- Index
CREATE INDEX idx_users_email          ON users(email);
CREATE INDEX idx_refresh_token        ON refresh_tokens(token);
CREATE INDEX idx_reset_token          ON password_reset_tokens(token);
CREATE INDEX idx_address_user         ON addresses(user_id);
