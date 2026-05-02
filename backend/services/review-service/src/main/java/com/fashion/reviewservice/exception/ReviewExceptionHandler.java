package com.fashion.reviewservice.exception;

import com.fashion.common.exception.BaseExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Review-service exception handler.
 * Inherits common handlers (IllegalArgument, IllegalState, Validation, catch-all)
 * from BaseExceptionHandler. Add review-specific handlers here if needed.
 */
@RestControllerAdvice
public class ReviewExceptionHandler extends BaseExceptionHandler {
    // All common handlers inherited from BaseExceptionHandler.
    // Add review-specific @ExceptionHandler methods here if needed.
}
