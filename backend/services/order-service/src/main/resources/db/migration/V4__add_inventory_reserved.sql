-- Add inventory_reserved flag for Saga race-condition fix.
-- null = result unknown yet, TRUE = inventory reserved, FALSE = reservation failed.
-- Idempotent: skip if column already exists.
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'inventory_reserved');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE orders ADD COLUMN inventory_reserved BOOLEAN DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
