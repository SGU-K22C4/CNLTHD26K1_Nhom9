CREATE TABLE reviews
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    VARCHAR(36) NOT NULL,
    product_id BIGINT      NOT NULL,
    rating     TINYINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    approved   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)          DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_user_product (user_id, product_id),
    INDEX idx_review_product (product_id),
    INDEX idx_review_approved (approved)
);
