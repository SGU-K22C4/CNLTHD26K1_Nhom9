-- ==============================================================================
-- MIGRATION SCRIPT: Fix incorrect category mappings for FEMALE products
-- ==============================================================================
-- Reason for migration:
-- The initial seed data or crawler incorrectly mapped the products in the FEMALE category.
-- Current incorrect mapping in DB:
-- - category_id = 5 (Váy/Dress) actually contains "Áo khoác" (Jacket) products.
-- - category_id = 6 (Áo khoác) actually contains "Áo sơ mi" (Shirt) products.
-- - category_id = 7 (Áo sơ mi) actually contains "Chân váy" (Skirt) products.
-- - category_id = 8 (Chân váy) actually contains "Đầm/Váy" (Dress) products.
--
-- This script shifts the category_ids cyclically to fix the mismatch:
-- 5 -> 6 (Move Jackets to Áo khoác)
-- 6 -> 7 (Move Shirts to Áo sơ mi)
-- 7 -> 8 (Move Skirts to Chân váy)
-- 8 -> 5 (Move Dresses to Váy/Dress)
-- ==============================================================================

UPDATE products
SET category_id = CASE
    WHEN category_id = '5' THEN '6'
    WHEN category_id = '6' THEN '7'
    WHEN category_id = '7' THEN '8'
    WHEN category_id = '8' THEN '5'
    ELSE category_id
END
WHERE category_id IN ('5', '6', '7', '8')
  AND EXISTS (
      SELECT 1 FROM (
          SELECT id FROM products 
          WHERE name LIKE '%KHOÁC%' AND category_id = '5' 
          LIMIT 1
      ) AS check_broken
  );
