package com.fashion.productservice.service.impl;

import com.fashion.productservice.dto.request.ProductRequest;
import com.fashion.productservice.dto.response.ProductResponse;
import com.fashion.productservice.entity.Category;
import com.fashion.productservice.entity.Product;
import com.fashion.productservice.repository.CategoryRepository;
import com.fashion.productservice.repository.ProductRepository;
import com.fashion.productservice.mapper.ProductMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

@Test
@DisplayName("Test lấy sản phẩm theo ID thành công")
void testGetById_Success() {
    String productId = "P123";
    Product mockProduct = Product.builder().id(productId).name("Áo thun").build();
    
    // Thay vì dùng 'new', ta dùng mock để tạo đối tượng giả
    ProductResponse mockResponse = mock(ProductResponse.class);
    lenient().when(mockResponse.getId()).thenReturn(productId);
    lenient().when(mockResponse.getName()).thenReturn("Áo thun");

    lenient().when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
    lenient().when(productMapper.toResponse(mockProduct)).thenReturn(mockResponse);

    ProductResponse result = productService.getById(productId);

    assertNotNull(result);
    assertEquals("Áo thun", result.getName());
}

    @Test
    @DisplayName("Test lỗi không tìm thấy sản phẩm")
    void testGetById_NotFound() {
        String productId = "UNKNOWN";
        lenient().when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.getById(productId));
    }
@Test
    @DisplayName("Test tạo sản phẩm mới thành công")
    void testCreate_Success() {
        // 1. Arrange (Chuẩn bị)
        ProductRequest request = new ProductRequest();
        request.setName("Quần Jean");
        request.setCategoryId("CAT01");
        request.setVariants(new ArrayList<>()); // Giả sử không có variant cho đơn giản

        Category mockCategory = Category.builder().id("CAT01").name("Thời trang").build();
        Product savedProduct = Product.builder().id("NEW_ID").name("Quần Jean").build();
        ProductResponse mockResponse = ProductResponse.builder().id("NEW_ID").name("Quần Jean").build();

        when(categoryRepository.findById("CAT01")).thenReturn(Optional.of(mockCategory));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toResponse(any(Product.class))).thenReturn(mockResponse);

        // 2. Act (Thực thi)
        ProductResponse result = productService.create(request);

        // 3. Assert (Kiểm chứng)
        assertNotNull(result);
        assertEquals("Quần Jean", result.getName());
        verify(productRepository).save(any(Product.class)); // Xác nhận có gọi lệnh save
    }

    @Test
    @DisplayName("Test cập nhật sản phẩm thành công")
    void testUpdate_Success() {
        // 1. Arrange
        String productId = "P123";
        ProductRequest updateRequest = new ProductRequest();
        updateRequest.setName("Tên mới");
        updateRequest.setCategoryId("CAT01");

        Product existingProduct = Product.builder()
                .id(productId)
                .name("Tên cũ")
                .variants(new ArrayList<>()) // Cần list thực để tránh lỗi khi .clear()
                .build();
        
        Category mockCategory = Category.builder().id("CAT01").build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById("CAT01")).thenReturn(Optional.of(mockCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productMapper.toResponse(any(Product.class))).thenReturn(ProductResponse.builder().name("Tên mới").build());

        // 2. Act
        ProductResponse result = productService.update(productId, updateRequest);

        // 3. Assert
        assertEquals("Tên mới", result.getName());
        verify(productRepository).save(existingProduct);
    }

    @Test
    @DisplayName("Test lấy danh sách sản phẩm có phân trang và filter")
    void testGetAll_Success() {
        // 1. Arrange
        Pageable pageable = PageRequest.of(0, 12);
        Page<Product> mockPage = new PageImpl<>(List.of(new Product()));

        when(productRepository.search(eq("CAT01"), eq("Áo"), any(), any(), eq(pageable)))
                .thenReturn(mockPage);
        when(productMapper.toResponse(any())).thenReturn(ProductResponse.builder().build());

        // 2. Act
        Page<ProductResponse> result = productService.getAll("CAT01", "Áo", null, null, pageable);

        // 3. Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(productRepository).search(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Test xóa sản phẩm")
    void testDelete_Success() {
        // 1. Arrange
        String productId = "P123";

        // 2. Act
        productService.delete(productId);

        // 3. Assert
        verify(productRepository, times(1)).deleteById(productId);
    }
}