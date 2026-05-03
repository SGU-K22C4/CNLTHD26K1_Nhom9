-- Remove unused 'district' column from addresses table
-- The Address entity only maps street, ward, and city.

SET @db_name = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'addresses' AND COLUMN_NAME = 'district') = 1,
    'ALTER TABLE addresses DROP COLUMN district',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
