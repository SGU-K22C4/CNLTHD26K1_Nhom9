CREATE TABLE coupons
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    code               VARCHAR(50)   UNIQUE NOT NULL,
    discount_type      ENUM('PERCENTAGE','FIXED_AMOUNT') NOT NULL,
    discount_value     DECIMAL(10,2) NOT NULL,
    min_order_amount   DECIMAL(12,2),
    max_discount_amount DECIMAL(12,2),
    usage_limit        INT,
    used_count         INT           NOT NULL DEFAULT 0,
    start_date         DATETIME      NOT NULL,
    end_date           DATETIME      NOT NULL,
    active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at         DATETIME(6)            DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_coupon_code (code),
    INDEX idx_coupon_active_dates (active, start_date, end_date)
);
