package com.fashion.common.util;

/**
 * Utility class for inter-service REST client operations.
 * Provides common helper methods used across service-to-service communication.
 */
public final class RestClientUtils {

    private RestClientUtils() {
        // utility class — prevent instantiation
    }

    /**
     * Extract the "message" field from a JSON error response body using simple string parsing.
     * Avoids pulling in Jackson for error-path-only deserialization.
     *
     * @param body     the raw JSON response body (may be null or empty)
     * @param fallback the fallback message if extraction fails
     * @return the extracted message, or the fallback
     */
    public static String extractMessage(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }

        String marker = "\"message\":\"";
        int start = body.indexOf(marker);
        if (start < 0) {
            return fallback;
        }
        start += marker.length();
        int end = body.indexOf('"', start);
        if (end <= start) {
            return fallback;
        }
        return body.substring(start, end);
    }
}
