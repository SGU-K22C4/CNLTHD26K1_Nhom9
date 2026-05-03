package com.fashion.productservice.exception;

import com.fashion.common.dto.response.ApiResponse;
import com.fashion.common.exception.BaseExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Product-service exception handler.
 * Inherits common handlers from BaseExceptionHandler and adds
 * a product-specific RuntimeException handler for "not found" detection.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends BaseExceptionHandler {

    /**
     * "Product not found", "Category not found" — from ProductServiceImpl.
     * Overrides the catch-all to differentiate "not found" RuntimeExceptions.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex) {
        String message = ex.getMessage();
        if (message != null && message.toLowerCase().contains("not found")) {
            return buildError(HttpStatus.NOT_FOUND, message);
        }
        log.error("Unexpected RuntimeException", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }
}
