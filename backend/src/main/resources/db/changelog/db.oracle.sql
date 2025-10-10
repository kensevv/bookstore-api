-- liquibase formatted sql

-- changeset create-user-table:1
CREATE TABLE users
(
    USERNAME      VARCHAR2(255) PRIMARY KEY,
    PASSWORD_HASH VARCHAR2(255)       NOT NULL,
    ROLE          VARCHAR2(20)        NOT NULL,
    CREATED_AT    TIMESTAMP           NOT NULL,
    UPDATED_AT    TIMESTAMP           NOT NULL,
    DELETED       CHAR(1) DEFAULT 'N' NOT NULL
);
