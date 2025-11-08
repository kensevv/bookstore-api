-- V4__create_orders_tables.sql

CREATE TABLE orders
(
    id               BIGSERIAL PRIMARY KEY,
    user_email       VARCHAR(255)   NOT NULL,
    order_number     VARCHAR(50)    NOT NULL UNIQUE,
    total_amount     DECIMAL(10, 2) NOT NULL,
    status           VARCHAR(20)    NOT NULL,
    shipping_address TEXT           NOT NULL,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_user ON orders (user_email);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items
(
    id                BIGSERIAL PRIMARY KEY,
    order_id          BIGINT         NOT NULL,
    book_id           BIGINT         NOT NULL,
    quantity          INTEGER        NOT NULL,
    price_at_purchase DECIMAL(10, 2) NOT NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE RESTRICT
);

CREATE INDEX idx_order_items_order ON order_items (order_id);
