CREATE TABLE categories
(
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    gender      ENUM('MALE', 'FEMALE') NOT NULL,
    is_visible  BOOLEAN DEFAULT TRUE,
    parent_id   VARCHAR(36),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
);

CREATE TABLE products
(
    id                  VARCHAR(36) PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    is_visible          BOOLEAN DEFAULT TRUE,
    category_id         VARCHAR(36),
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE product_variants
(
    id                 VARCHAR(36) PRIMARY KEY,
    color_name         VARCHAR(36) NOT NULL,
    price              DECIMAL(15, 2) NOT NULL,
    composition_detail VARCHAR(500),
    product_url        VARCHAR(500),
    product_id         VARCHAR(36) NOT NULL,
    created_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE TABLE variant_images
(
    id         VARCHAR(36) PRIMARY KEY,
    variant_id VARCHAR(36) NOT NULL,
    image_url  VARCHAR(500) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0,
    CONSTRAINT fk_image_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE
);

CREATE TABLE variant_sizes
(
    id         VARCHAR(36) PRIMARY KEY,
    variant_id VARCHAR(36) NOT NULL,
    size_name  VARCHAR(10) NOT NULL,
    quantity   INT DEFAULT 0,
    status     VARCHAR(20) DEFAULT 'Con hang',
    CONSTRAINT fk_size_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE
);
