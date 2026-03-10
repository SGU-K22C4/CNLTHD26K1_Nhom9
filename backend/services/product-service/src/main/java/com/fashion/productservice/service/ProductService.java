package com.fashion.productservice.service;

import com.fashion.productservice.dto.request.ProductRequest;
import com.fashion.productservice.dto.response.ProductResponse;
import com.fashion.productservice.entity.Category;
import com.fashion.productservice.entity.Product;
import com.fashion.productservice.entity.ProductImage;
import com.fashion.productservice.entity.ProductVariant;
import com.fashion.productservice.repository.CategoryRepository;
import com.fashion.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(String categorySlug, String search,
                                        BigDecimal minPrice, BigDecimal maxPrice,
                                        Pageable pageable) {
        return productRepository.search(categorySlug, search, minPrice, maxPrice, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Product not found: " + slug));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getFeatured(Pageable pageable) {
        return productRepository.findByFeaturedTrue(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getNewArrivals(Pageable pageable) {
        return productRepository.findByNewArrivalTrue(pageable).map(this::toResponse);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .slug(generateSlug(request.getName()))
                .description(request.getDescription())
                .materials(request.getMaterials())
                .careInstructions(request.getCareInstructions())
                .price(request.getPrice())
                .salePrice(request.getSalePrice())
                .category(category)
                .featured(request.isFeatured())
                .newArrival(request.isNewArrival())
                .build();

        if (request.getImages() != null) {
            request.getImages().forEach(img -> {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .url(img.getUrl())
                        .altText(img.getAltText())
                        .primary(img.isPrimary())
                        .sortOrder(img.getSortOrder())
                        .build();
                product.getImages().add(image);
            });
        }

        if (request.getVariants() != null) {
            request.getVariants().forEach(v -> {
                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .color(v.getColor())
                        .colorHex(v.getColorHex())
                        .size(v.getSize())
                        .stock(v.getStock())
                        .sku(v.getSku())
                        .build();
                product.getVariants().add(variant);
            });
        }

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found: " + request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setMaterials(request.getMaterials());
        product.setCareInstructions(request.getCareInstructions());
        product.setPrice(request.getPrice());
        product.setSalePrice(request.getSalePrice());
        product.setCategory(category);
        product.setFeatured(request.isFeatured());
        product.setNewArrival(request.isNewArrival());

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .materials(product.getMaterials())
                .careInstructions(product.getCareInstructions())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .categorySlug(product.getCategory() != null ? product.getCategory().getSlug() : null)
                .status(product.getStatus().name())
                .featured(product.isFeatured())
                .newArrival(product.isNewArrival())
                .images(product.getImages().stream()
                        .map(img -> ProductResponse.ImageDto.builder()
                                .id(img.getId())
                                .url(img.getUrl())
                                .altText(img.getAltText())
                                .primary(img.isPrimary())
                                .build())
                        .toList())
                .variants(product.getVariants().stream()
                        .map(v -> ProductResponse.VariantDto.builder()
                                .id(v.getId())
                                .color(v.getColor())
                                .colorHex(v.getColorHex())
                                .size(v.getSize())
                                .stock(v.getStock())
                                .sku(v.getSku())
                                .build())
                        .toList())
                .createdAt(product.getCreatedAt())
                .build();
    }

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name.toLowerCase(), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized)
                .replaceAll("")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
