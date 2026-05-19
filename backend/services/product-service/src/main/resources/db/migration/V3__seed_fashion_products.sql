-- Seed Data for Categories
INSERT INTO categories (id, name, gender, is_visible) VALUES 
('cat-001', 'Áo Sơ Mi Nam', 'MALE', TRUE),
('cat-002', 'Quần Jean Nam', 'MALE', TRUE),
('cat-003', 'Đầm Nữ', 'FEMALE', TRUE),
('cat-004', 'Áo Thun Nữ', 'FEMALE', TRUE);

-- Seed Data for Products
INSERT INTO products (id, name, description, category_id) VALUES 
('prod-001', 'Áo Sơ Mi Oxford Slim Fit', 'Áo sơ mi Oxford chất liệu cotton cao cấp, thấm hút mồ hôi tốt, form dáng Slim Fit hiện đại.', 'cat-001'),
('prod-002', 'Quần Jean Slim-fit Dark Blue', 'Quần jean nam màu xanh đậm, chất liệu denim co giãn, bền màu, phù hợp mặc đi làm và đi chơi.', 'cat-002'),
('prod-003', 'Đầm Bút Chì Công Sở', 'Đầm dáng bút chì thanh lịch, chất vải lụa tổng hợp cao cấp, tôn dáng cho phái đẹp.', 'cat-003'),
('prod-004', 'Áo Thun Cotton Oversize', 'Áo thun cotton 100% co giãn 4 chiều, thiết kế oversize cá tính, thoải mái vận động.', 'cat-004');

-- Seed Data for Product Variants
-- Mỗi sản phẩm có 2 màu (đen/trắng hoặc xanh/xám)
INSERT INTO product_variants (id, product_id, color_name, price, composition_detail) VALUES 
('var-001', 'prod-001', 'Trắng', 450000.00, '100% Cotton Oxford'),
('var-002', 'prod-001', 'Xanh Nhạt', 450000.00, '100% Cotton Oxford'),
('var-003', 'prod-002', 'Xanh Đậm', 750000.00, '98% Cotton, 2% Spandex'),
('var-004', 'prod-003', 'Đen', 1200000.00, 'Silk Blend'),
('var-005', 'prod-003', 'Đỏ Rượu', 1350000.00, 'Premium Silk'),
('var-006', 'prod-004', 'Trắng', 250000.00, '100% Cotton');

-- Seed Data for Variant Images (Dùng ảnh placeholder chất lượng cao)
INSERT INTO variant_images (id, variant_id, image_url, is_primary, sort_order) VALUES 
('img-001', 'var-001', 'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?auto=format&fit=crop&w=800&q=80', TRUE, 0),
('img-002', 'var-003', 'https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=800&q=80', TRUE, 0),
('img-003', 'var-004', 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?auto=format&fit=crop&w=800&q=80', TRUE, 0),
('img-004', 'var-006', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80', TRUE, 0);

-- Seed Data for Variant Sizes
INSERT INTO variant_sizes (id, variant_id, size_name, quantity, status) VALUES 
(UUID(), 'var-001', 'S', 20, 'Con hang'),
(UUID(), 'var-001', 'M', 50, 'Con hang'),
(UUID(), 'var-001', 'L', 30, 'Con hang'),
(UUID(), 'var-003', '30', 15, 'Con hang'),
(UUID(), 'var-003', '32', 25, 'Con hang'),
(UUID(), 'var-004', 'M', 10, 'Con hang'),
(UUID(), 'var-004', 'L', 5, 'Con hang');
