package com.fashion.productservice.service;

import com.fashion.productservice.dto.request.ProductRequest;
import com.fashion.productservice.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {

    Page<ProductResponse> getAll(String categoryId, String search,
                                 BigDecimal minPrice, BigDecimal maxPrice,
                                 Pageable pageable);

    ProductResponse getById(String id);

    Page<ProductResponse> getFeatured(Pageable pageable);

    Page<ProductResponse> getNewArrivals(Pageable pageable);

    ProductResponse create(ProductRequest request);

    ProductResponse update(String id, ProductRequest request);

    void delete(String id);
}
