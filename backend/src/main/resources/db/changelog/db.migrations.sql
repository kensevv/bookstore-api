-- liquibase formatted sql

-- changeset create-user-table:1
CREATE TABLE users
(
    username      VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(255)          NOT NULL,
    role          VARCHAR(20)           NOT NULL,
    created_at    timestamp             NOT NULL,
    updated_at    timestamp             NOT NULL,
    deleted       BOOLEAN default false not null
);
