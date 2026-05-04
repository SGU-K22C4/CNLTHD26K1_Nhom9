package com.fashion.common.exception;

import com.fashion.common.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

/**
 * Base exception handler providing common error handling for all microservices.
 * <p>
 * Each service should extend this class with {@code @RestControllerAdvice} and
 * add service-specific exception handlers as needed (e.g. Redis errors for cart,
 * auth errors for user).
 * <p>
 * <b>Important:</b> Do NOT annotate this base class with {@code @RestControllerAdvice}.
 * Only the concrete subclass in each service should carry that annotation.
 *
 * <pre>
 * {@code
 * @RestControllerAdvice
 * public class MyServiceExceptionHandler extends BaseExceptionHandler {
 *     // Add service-specific @ExceptionHandler methods here
 * }
 * }
 * </pre>
 */
@Slf4j
public abstract class BaseExceptionHandler {

    /**
     * Validation errors from {@code @Valid} on request DTOs.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildError(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Request structure errors raised by Spring before controller logic runs.
     * These should be 400 instead of falling through to the generic 500 handler.
     */
    @ExceptionHandler({
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception ex) {
        log.warn("Malformed request: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, resolveMalformedRequestMessage(ex));
    }

    /**
     * Business logic validation errors (invalid input, resource not found, etc.).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Request validation failed: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Downstream service unavailable or system state errors.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceError(IllegalStateException ex) {
        log.error("Service state error: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    /**
     * Catch-all for unexpected errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    /**
     * Build a standardized error response.
     * Protected so subclasses can reuse for service-specific handlers.
     */
    protected ResponseEntity<ApiResponse<Void>> buildError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }

    /**
     * Hook for service-specific malformed request messages without adding
     * another @ExceptionHandler that would duplicate the base mapping.
     */
    protected String resolveMalformedRequestMessage(Exception ex) {
        return ex.getMessage();
    }
}
