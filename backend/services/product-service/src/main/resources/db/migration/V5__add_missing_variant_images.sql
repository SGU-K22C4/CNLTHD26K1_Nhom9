-- Update image URL for var-004 to premium dress image
UPDATE variant_images 
SET image_url = 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?auto=format&fit=crop&w=800&q=80'
WHERE id = 'img-003';

-- Insert new variant image for var-006 (white t-shirt)
INSERT INTO variant_images (id, variant_id, image_url, is_primary, sort_order) VALUES 
('img-004', 'var-006', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80', TRUE, 0)
ON DUPLICATE KEY UPDATE image_url = VALUES(image_url);
