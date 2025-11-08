-- V2__create_books_table.sql

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