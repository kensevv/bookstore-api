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

-- changeset carts:1
CREATE TABLE shopping_carts
(
    id         BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_carts_user FOREIGN KEY (user_email) REFERENCES users (email) ON DELETE CASCADE
);

-- changeset carts:2
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

-- changeset orders:1
CREATE TABLE orders
(
    id               BIGSERIAL PRIMARY KEY,
    user_email       VARCHAR(255)   NOT NULL,
    order_number     VARCHAR(50)    NOT NULL UNIQUE,
    total_amount     DECIMAL(10, 2) NOT NULL,
    status           VARCHAR(20)    NOT NULL,
    shipping_address TEXT           NOT NULL,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_email) REFERENCES users (email) ON DELETE RESTRICT
);

CREATE INDEX idx_orders_user ON orders (user_email);
CREATE INDEX idx_orders_status ON orders (status);

-- changeset orders:2
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