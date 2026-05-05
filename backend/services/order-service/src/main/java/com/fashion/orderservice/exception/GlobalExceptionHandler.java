package com.fashion.orderservice.exception;

import com.fashion.common.exception.BaseExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Order-service exception handler.
 * Inherits common handlers (IllegalArgument, IllegalState, Validation, catch-all)
 * from BaseExceptionHandler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseExceptionHandler {
    // All common handlers inherited from BaseExceptionHandler.
    // Add order-specific @ExceptionHandler methods here if needed.
}
