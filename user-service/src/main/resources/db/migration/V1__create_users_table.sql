-- V1__create_users_table.sql

CREATE TABLE IF NOT EXISTS users
(
    email         VARCHAR(255) PRIMARY KEY,
    first_name    VARCHAR(100)                                          NOT NULL,
    last_name     VARCHAR(100)                                          NOT NULL,
    password_hash VARCHAR(255)                                          NOT NULL,
    role          VARCHAR(50)                                           NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted       BOOLEAN                                               NOT NULL DEFAULT FALSE
);
