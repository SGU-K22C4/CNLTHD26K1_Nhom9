package com.fashion.chatbotservice.controller;

import com.fashion.chatbotservice.dto.ChatFeedbackEventRequest;
import com.fashion.chatbotservice.service.ChatFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chatbot/analytics")
@RequiredArgsConstructor
public class ChatAnalyticsController {

    private final ChatFeedbackService chatFeedbackService;

    @Value("${chatbot.internal.shared-secret:local-chatbot-internal-secret}")
    private String internalSharedSecret;

    @Value("${chatbot.security.allow-direct-user-header:true}")
    private boolean allowDirectUserHeader;

    @PostMapping("/events")
    public ResponseEntity<Map<String, String>> recordEvent(
            @RequestBody ChatFeedbackEventRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-Consumer-Username", required = false) String consumerUsername,
            @RequestHeader(value = "X-Internal-Caller", required = false) String internalCaller,
            @RequestHeader(value = "X-Internal-Auth", required = false) String internalAuth) {

        chatFeedbackService.recordEvent(
                resolveTrustedUserId(userId, userRole, consumerUsername, internalCaller, internalAuth),
                request);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

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
        return null;
    }

    private boolean isTrustedInternalCall(String internalCaller, String internalAuth) {
        return internalCaller != null
                && !internalCaller.isBlank()
                && internalAuth != null
                && internalAuth.equals(internalSharedSecret);
    }
}
