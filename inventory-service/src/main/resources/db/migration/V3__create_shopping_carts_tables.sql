-- V3__create_shopping_carts_tables.sql

CREATE TABLE shopping_carts
(
    id         BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shopping_carts_items
(
    id         BIGSERIAL PRIMARY KEY,
    cart_id    BIGINT    NOT NULL,
    book_id    BIGINT    NOT NULL,
    quantity   INTEGER   NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES shopping_carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,
    CONSTRAINT uq_cart_items_cart_book UNIQUE (cart_id, book_id)
);

CREATE INDEX idx_cart_items_cart ON shopping_carts_items (cart_id);
CREATE INDEX idx_cart_items_book ON shopping_carts_items (book_id);
