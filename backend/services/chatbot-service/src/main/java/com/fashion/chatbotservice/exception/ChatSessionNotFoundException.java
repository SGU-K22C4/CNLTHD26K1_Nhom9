package com.fashion.chatbotservice.exception;

/**
 * Thrown when a chat session is not found in MongoDB.
 * Maps to HTTP 404 via ChatbotExceptionHandler.
 */
public class ChatSessionNotFoundException extends RuntimeException {

    public ChatSessionNotFoundException(String message) {
        super(message);
    }
}
