package com.fashion.promotionservice.exception;

import com.fashion.common.exception.BaseExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Promotion-service exception handler.
 * Inherits common handlers (IllegalArgument, IllegalState, Validation, catch-all)
 * from BaseExceptionHandler. Add promotion-specific handlers here if needed.
 */
@RestControllerAdvice
public class PromotionExceptionHandler extends BaseExceptionHandler {
    // All common handlers inherited from BaseExceptionHandler.
    // Add promotion-specific @ExceptionHandler methods here if needed.
}
