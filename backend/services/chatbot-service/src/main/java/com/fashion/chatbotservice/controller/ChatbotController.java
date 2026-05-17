package com.fashion.chatbotservice.controller;

import com.fashion.chatbotservice.dto.ChatRequest;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.dto.SessionResponse;
import com.fashion.chatbotservice.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;

    @Value("${chatbot.internal.shared-secret:local-chatbot-internal-secret}")
    private String internalSharedSecret;

    @Value("${chatbot.security.allow-direct-user-header:true}")
    private boolean allowDirectUserHeader;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-Consumer-Username", required = false) String consumerUsername,
            @RequestHeader(value = "X-Internal-Caller", required = false) String internalCaller,
            @RequestHeader(value = "X-Internal-Auth", required = false) String internalAuth) {

        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);

        try {
            log.info("Chat request: sessionId={}, messageLength={}",
                    request.getSessionId(),
                    request.getMessage() != null ? request.getMessage().length() : 0);

            ChatResponse response = chatbotService.chat(
                    request,
                    resolveTrustedUserId(userId, userRole, consumerUsername, internalCaller, internalAuth),
                    traceId);
            return ResponseEntity.ok(response);
        } finally {
            MDC.remove("traceId");
        }
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatbotService.getSession(sessionId));
    }

    /**
     * Chat vẫn public cho guest mode, nhưng user context chỉ được tin khi request đi
     * qua gateway/internal trust hợp lệ. Làm vậy để tránh client tự spoof X-User-Id.
     */
    private String resolveTrustedUserId(String userId,
                                        String userRole,
                                        String consumerUsername,
                                        String internalCaller,
                                        String internalAuth) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        if (isTrustedInternalCall(internalCaller, internalAuth)) {
            return userId.trim();
        }
        if ((userRole != null && !userRole.isBlank())
                || (consumerUsername != null && !consumerUsername.isBlank())) {
            return userId.trim();
        }
        if (allowDirectUserHeader) {
            return userId.trim();
        }
        log.debug("Ignoring untrusted X-User-Id header for chatbot request");
        return null;
    }

    private boolean isTrustedInternalCall(String internalCaller, String internalAuth) {
        return internalCaller != null
                && !internalCaller.isBlank()
                && internalAuth != null
                && internalAuth.equals(internalSharedSecret);
    }
}
