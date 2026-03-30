CREATE TABLE wishlists (
    wishlist_id   VARCHAR(36) PRIMARY KEY,
    user_id       VARCHAR(36) NOT NULL,
    product_id    VARCHAR(36) NOT NULL,
    created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wishlist_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uq_wishlist UNIQUE (user_id, product_id)
);
