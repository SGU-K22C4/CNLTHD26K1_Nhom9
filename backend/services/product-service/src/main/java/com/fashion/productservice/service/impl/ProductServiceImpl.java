package com.fashion.productservice.service.impl;

import com.fashion.productservice.dto.request.ProductRequest;
import com.fashion.productservice.dto.response.ProductResponse;
import com.fashion.productservice.entity.Category;
import com.fashion.productservice.entity.Product;
import com.fashion.productservice.entity.ProductVariant;
import com.fashion.productservice.entity.VariantImage;
import com.fashion.productservice.entity.VariantSize;
import com.fashion.productservice.mapper.ProductMapper;
import com.fashion.productservice.repository.CategoryRepository;
import com.fashion.productservice.repository.ProductRepository;
import com.fashion.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(String categoryId, String search,
            BigDecimal minPrice, BigDecimal maxPrice,
            Pageable pageable) {
        return productRepository.search(categoryId, search, minPrice, maxPrice, pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAdminProducts(String categoryId, String search,
            BigDecimal minPrice, BigDecimal maxPrice,
            String status,
            Pageable pageable) {
        return productRepository.searchAdmin(categoryId, search, minPrice, maxPrice, status, pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(String id) {
        return productRepository.findById(id)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getFeatured(Pageable pageable) {
        return productRepository.findByVisibleTrue(pageable).map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getNewArrivals(Pageable pageable) {
        return productRepository.findByVisibleTrueOrderByCreatedAtDesc(pageable).map(productMapper::toResponse);
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found: " + request.getCategoryId()));

        Product product = Product.builder()
                .id(newId())
                .name(request.getName())
                .description(request.getDescription())
                .visible(request.isVisible())
                .category(category)
                .build();

        if (request.getVariants() != null) {
            request.getVariants().forEach(v -> {
                ProductVariant variant = ProductVariant.builder()
                        .id(newId())
                        .product(product)
                        .colorName(v.getColorName())
                        .price(v.getPrice())
                        .compositionDetail(v.getCompositionDetail())
                        .productUrl(v.getProductUrl())
                        .build();

                if (v.getImages() != null) {
                    v.getImages().forEach(img -> {
                        VariantImage image = VariantImage.builder()
                                .id(newId())
                                .variant(variant)
                                .imageUrl(img.getImageUrl())
                                .primary(img.isPrimary())
                                .sortOrder(img.getSortOrder())
                                .build();
                        variant.getImages().add(image);
                    });
                }

                if (v.getSizes() != null) {
                    v.getSizes().forEach(s -> {
                        VariantSize size = VariantSize.builder()
                                .id(newId())
                                .variant(variant)
                                .sizeName(s.getSizeName())
                                .quantity(s.getQuantity())
                                .status(s.getStatus() == null || s.getStatus().isBlank() ? "Con hang" : s.getStatus())
                                .build();
                        variant.getSizes().add(size);
                    });
                }

                product.getVariants().add(variant);
            });
        }

        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(String id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found: " + request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setVisible(request.isVisible());
        product.setCategory(category);

        product.getVariants().clear();
        if (request.getVariants() != null) {
            request.getVariants().forEach(v -> {
                ProductVariant variant = ProductVariant.builder()
                        .id(newId())
                        .product(product)
                        .colorName(v.getColorName())
                        .price(v.getPrice())
                        .compositionDetail(v.getCompositionDetail())
                        .productUrl(v.getProductUrl())
                        .build();

                if (v.getImages() != null) {
                    v.getImages().forEach(img -> variant.getImages().add(
                            VariantImage.builder()
                                    .id(newId())
                                    .variant(variant)
                                    .imageUrl(img.getImageUrl())
                                    .primary(img.isPrimary())
                                    .sortOrder(img.getSortOrder())
                                    .build()));
                }

                if (v.getSizes() != null) {
                    v.getSizes().forEach(s -> variant.getSizes().add(
                            VariantSize.builder()
                                    .id(newId())
                                    .variant(variant)
                                    .sizeName(s.getSizeName())
                                    .quantity(s.getQuantity())
                                    .status(s.getStatus() == null || s.getStatus().isBlank() ? "Con hang"
                                            : s.getStatus())
                                    .build()));
                }

                product.getVariants().add(variant);
            });
        }

        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(String id) {
        productRepository.deleteById(id);
    }

    private String newId() {
        return UUID.randomUUID().toString();
    }
}
