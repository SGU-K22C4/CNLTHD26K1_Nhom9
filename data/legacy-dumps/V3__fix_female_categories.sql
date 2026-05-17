-- ==============================================================================
-- MIGRATION SCRIPT: Fix incorrect category mappings for FEMALE products
-- ==============================================================================
-- Why this version exists:
-- The previous script depended on a text-based EXISTS guard with broken encoding.
-- When that guard failed, the UPDATE silently did nothing even though the female
-- categories were still cyclically mismatched.
--
-- Current mismatch confirmed in DB:
--   5 (Vay/Dress)  -> actually contains Ao khoac
--   6 (Ao khoac)   -> actually contains Ao so mi
--   7 (Ao so mi)   -> actually contains Chan vay
--   8 (Chan vay)   -> actually contains Dam/Vay
--
-- Because the mismatch is deterministic, this migration rotates all female
-- product category ids directly and does not rely on name matching.
-- ==============================================================================

START TRANSACTION;

UPDATE products
SET category_id = CASE
    WHEN category_id = '5' THEN '6'
    WHEN category_id = '6' THEN '7'
    WHEN category_id = '7' THEN '8'
    WHEN category_id = '8' THEN '5'
    ELSE category_id
END
WHERE category_id IN ('5', '6', '7', '8');

COMMIT;

-- Quick verification summary after the rotation.
SELECT
    c.id AS category_id,
    c.name AS category_name,
    COUNT(*) AS total_products
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE c.gender = 'FEMALE'
GROUP BY c.id, c.name
ORDER BY c.id;

-- Sampling rows helps confirm visible product names now align with category.
SELECT
    p.id,
    p.name,
    p.category_id,
    c.name AS category_name
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE c.gender = 'FEMALE'
ORDER BY p.category_id, p.name
LIMIT 20;
