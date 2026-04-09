CREATE TABLE membership_tiers (
    tier_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    min_spending DECIMAL(15,2) NOT NULL,
    discount_percent DECIMAL(5,2) NOT NULL,
    point_rate DECIMAL(5,2) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO membership_tiers (tier_id, name, min_spending, discount_percent, point_rate)
VALUES
('tier-1', 'Bronze', 0, 0, 1.0),
('tier-2', 'Silver', 5000000, 3, 1.2),
('tier-3', 'Gold', 20000000, 5, 1.5),
('tier-4', 'Platinum', 50000000, 8, 2.0)
ON DUPLICATE KEY UPDATE tier_id = VALUES(tier_id);

CREATE TABLE user_loyalty (
    loyalty_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    tier_id VARCHAR(36) NOT NULL,
    current_points INT DEFAULT 0,
    total_spending DECIMAL(15,2) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_loyalty_tier FOREIGN KEY (tier_id) REFERENCES membership_tiers(tier_id)
);

CREATE TABLE point_transactions (
    transaction_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    type ENUM('EARN_ORDER', 'EARN_REVIEW', 'REDEEM', 'REFUND') NOT NULL,
    points INT NOT NULL,
    ref_id VARCHAR(64) NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_point_tx_user_type_ref (user_id, type, ref_id),
    INDEX idx_point_tx_user_created_at (user_id, created_at)
);
