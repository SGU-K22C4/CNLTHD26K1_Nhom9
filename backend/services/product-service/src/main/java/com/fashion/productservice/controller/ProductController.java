package com.fashion.productservice.controller;

import com.fashion.productservice.dto.request.ProductRequest;
import com.fashion.productservice.dto.response.ProductResponse;
import com.fashion.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAll(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(
            productService.getAll(categoryId, search, minPrice, maxPrice, page, size, sortBy, sortDir)
        );
    }

    @GetMapping("/featured")
    public ResponseEntity<Page<ProductResponse>> getFeatured(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        return ResponseEntity.ok(productService.getFeatured(PageRequest.of(page, size)));
    }

    @GetMapping("/new-arrivals")
    public ResponseEntity<Page<ProductResponse>> getNewArrivals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        return ResponseEntity.ok(productService.getNewArrivals(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping("/admin/list")
    public ResponseEntity<Page<ProductResponse>> getAdminProducts(
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        assertAdminRole(userRole);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(
                productService.getAdminProducts(categoryId, search, minPrice, maxPrice, status, PageRequest.of(page, size, sort))
        );
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody ProductRequest request) {
        assertAdminRole(userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable String id,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody ProductRequest request) {
        assertAdminRole(userRole);
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable String id,
            @RequestHeader("X-User-Role") String userRole) {
        assertAdminRole(userRole);
        productService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    private void assertAdminRole(String userRole) {
        if (userRole == null || !("ADMIN".equalsIgnoreCase(userRole.trim()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required");
        }
    }
}
