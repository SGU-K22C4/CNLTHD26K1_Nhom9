ALTER TABLE orders
    ADD COLUMN loyalty_discount DECIMAL(12,2) DEFAULT 0 AFTER discount,
    ADD COLUMN used_points INT DEFAULT 0 AFTER loyalty_discount;
