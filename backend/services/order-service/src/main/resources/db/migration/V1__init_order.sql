CREATE TABLE orders
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number     VARCHAR(50)   UNIQUE NOT NULL,
    user_id          VARCHAR(36)   NOT NULL,
    status           ENUM('PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED','RETURNED') NOT NULL DEFAULT 'PENDING',
    subtotal         DECIMAL(12,2) NOT NULL,
    shipping_fee     DECIMAL(12,2)        DEFAULT 0,
    discount         DECIMAL(12,2)        DEFAULT 0,
    total            DECIMAL(12,2) NOT NULL,
    coupon_code      VARCHAR(50),
    recipient_name   VARCHAR(150),
    recipient_phone  VARCHAR(20),
    shipping_address TEXT,
    payment_method   ENUM('COD','BANK_TRANSFER','VNPAY','MOMO') DEFAULT 'COD',
    payment_status   ENUM('PENDING','PAID','FAILED','REFUNDED')  DEFAULT 'PENDING',
    note             TEXT,
    created_at       DATETIME(6)          DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)          DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_order_user (user_id),
    INDEX idx_order_status (status)
);

CREATE TABLE order_items
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT        NOT NULL,
    product_id   BIGINT        NOT NULL,
    product_name VARCHAR(200)  NOT NULL,
    product_slug VARCHAR(250),
    image_url    VARCHAR(500),
    color        VARCHAR(50),
    size         VARCHAR(20),
    quantity     INT           NOT NULL,
    unit_price   DECIMAL(12,2) NOT NULL,
    total_price  DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);
