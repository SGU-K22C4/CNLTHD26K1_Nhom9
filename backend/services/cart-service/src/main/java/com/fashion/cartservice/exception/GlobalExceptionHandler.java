package com.fashion.cartservice.exception;

import com.fashion.common.dto.response.ApiResponse;
import com.fashion.common.exception.BaseExceptionHandler;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Cart-service exception handler.
 * Inherits common handlers from BaseExceptionHandler and adds
 * cart-specific handlers for Redis plus custom malformed-request messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseExceptionHandler {

    /**
     * Keep the actual exception mapping in the base class so Spring only sees
     * one handler for MissingRequestHeaderException during startup.
     */
    @Override
    protected String resolveMalformedRequestMessage(Exception ex) {
        if (ex instanceof MissingRequestHeaderException missingHeaderException) {
            return "Missing required header: " + missingHeaderException.getHeaderName();
        }
        return super.resolveMalformedRequestMessage(ex);
    }

    /**
     * Redis connection failure.
     */
    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedisDown(RedisConnectionFailureException ex) {
        return buildError(HttpStatus.SERVICE_UNAVAILABLE, "Cart service temporarily unavailable");
    }
}
