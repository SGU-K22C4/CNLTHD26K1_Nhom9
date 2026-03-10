CREATE TABLE categories
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(150) UNIQUE NOT NULL,
    description TEXT,
    image_url   VARCHAR(500),
    parent_id   BIGINT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INT                   DEFAULT 0,
    created_at  DATETIME(6)           DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)           DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
);

CREATE TABLE products
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(200)   NOT NULL,
    slug                VARCHAR(250) UNIQUE NOT NULL,
    description         TEXT,
    materials           TEXT,
    care_instructions   TEXT,
    price               DECIMAL(12, 2) NOT NULL,
    sale_price          DECIMAL(12, 2),
    category_id         BIGINT         NOT NULL,
    status              ENUM('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK') NOT NULL DEFAULT 'ACTIVE',
    is_featured         BOOLEAN                                             DEFAULT FALSE,
    is_new              BOOLEAN                                             DEFAULT FALSE,
    created_at          DATETIME(6)                                         DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)                                         DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE product_images
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT       NOT NULL,
    url        VARCHAR(500) NOT NULL,
    alt_text   VARCHAR(200),
    is_primary BOOLEAN DEFAULT FALSE,
    sort_order INT     DEFAULT 0,
    CONSTRAINT fk_image_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE TABLE product_variants
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT      NOT NULL,
    color      VARCHAR(50),
    color_hex  VARCHAR(10),
    size       VARCHAR(20),
    stock      INT         NOT NULL DEFAULT 0,
    sku        VARCHAR(100) UNIQUE,
    CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

-- Seed root categories
INSERT INTO categories (name, slug, sort_order) VALUES ('Nữ', 'nu', 1), ('Nam', 'nam', 2), ('Phụ Kiện', 'phu-kien', 3);
