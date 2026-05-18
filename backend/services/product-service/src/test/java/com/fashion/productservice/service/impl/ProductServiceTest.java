package com.fashion.productservice.service.impl;

import com.fashion.productservice.dto.request.ProductRequest;
import com.fashion.productservice.dto.response.ProductResponse;
import com.fashion.productservice.entity.Category;
import com.fashion.productservice.entity.Product;
import com.fashion.productservice.mapper.ProductMapper;
import com.fashion.productservice.repository.CategoryRepository;
import com.fashion.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void should_ReturnMappedResponse_When_ProductExists() {
        String productId = "P123";
        Product product = Product.builder().id(productId).name("Ao thun").build();
        ProductResponse expectedResponse = ProductResponse.builder().id(productId).name("Ao thun").build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(expectedResponse);

        ProductResponse actualResponse = productService.getById(productId);

        assertEquals(expectedResponse.getId(), actualResponse.getId());
        assertEquals(expectedResponse.getName(), actualResponse.getName());
    }

    @Test
    void should_ThrowRuntimeException_When_ProductDoesNotExist() {
        String productId = "UNKNOWN";
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> productService.getById(productId));

        assertEquals("Product not found: " + productId, exception.getMessage());
    }

    @Test
    void should_CreateProduct_When_CategoryExists() {
        ProductRequest request = new ProductRequest();
        request.setName("Quan Jean");
        request.setCategoryId("CAT01");
        request.setVariants(new ArrayList<>());

        Category category = Category.builder().id("CAT01").name("Thoi trang").build();
        Product savedProduct = Product.builder().id("NEW_ID").name("Quan Jean").build();
        ProductResponse expectedResponse = ProductResponse.builder().id("NEW_ID").name("Quan Jean").build();

        when(categoryRepository.findById("CAT01")).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toResponse(savedProduct)).thenReturn(expectedResponse);

        ProductResponse actualResponse = productService.create(request);

        assertEquals(expectedResponse.getId(), actualResponse.getId());
        assertEquals(expectedResponse.getName(), actualResponse.getName());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void should_UpdateProduct_When_ProductAndCategoryExist() {
        String productId = "P123";
        ProductRequest request = new ProductRequest();
        request.setName("Ten moi");
        request.setCategoryId("CAT01");

        Product existingProduct = Product.builder()
                .id(productId)
                .name("Ten cu")
                .variants(new ArrayList<>())
                .build();
        Category category = Category.builder().id("CAT01").build();
        ProductResponse expectedResponse = ProductResponse.builder().id(productId).name("Ten moi").build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById("CAT01")).thenReturn(Optional.of(category));
        when(productRepository.save(existingProduct)).thenReturn(existingProduct);
        when(productMapper.toResponse(existingProduct)).thenReturn(expectedResponse);

        ProductResponse actualResponse = productService.update(productId, request);

        assertEquals(expectedResponse.getName(), actualResponse.getName());
        verify(productRepository).save(existingProduct);
    }

    @Test
    void should_ReturnPagedProducts_When_SearchFiltersProvided() {
        int page = 0;
        int size = 12;
        String sortBy = "createdAt";
        String sortDir = "desc";
        Product product = Product.builder().id("P1").build();
        ProductResponse mappedResponse = ProductResponse.builder().id("P1").build();
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(productRepository.search(eq("CAT01"), eq("Ao"), any(), any(), any(Pageable.class)))
            .thenReturn(productPage);
        when(productMapper.toResponse(product)).thenReturn(mappedResponse);

        Page<ProductResponse> result = productService.getAll(
            "CAT01",
            "Ao",
            null,
            null,
            page,
            size,
            sortBy,
            sortDir
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("P1", result.getContent().get(0).getId());
    }

    @Test
    void should_DeleteProductById_When_DeleteIsRequested() {
        String productId = "P123";

        productService.delete(productId);

        verify(productRepository).deleteById(productId);
    }
}
