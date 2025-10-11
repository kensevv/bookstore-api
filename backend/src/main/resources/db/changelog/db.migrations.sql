-- liquibase formatted sql

-- changeset create-user-table:1
CREATE TABLE users
(
    email         VARCHAR(255) PRIMARY KEY,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       BOOLEAN               default false not null
);

-- changeset create-categories:1
CREATE TABLE categories
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- changeset create-books:1
CREATE TABLE books
(
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255)   NOT NULL,
    author          VARCHAR(255)   NOT NULL,
    description     TEXT,
    price           DECIMAL(10, 2) NOT NULL,
    stock           INTEGER        NOT NULL,
    category_id     BIGINT         NOT NULL,
    cover_image_url VARCHAR(1000),
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN        NOT NULL DEFAULT false,
    CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
);

CREATE INDEX idx_books_title ON books (title);
CREATE INDEX idx_books_author ON books (author);
