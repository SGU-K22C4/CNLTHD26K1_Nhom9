package com.fashion.chatbotservice.controller;

import com.fashion.chatbotservice.dto.ChatRequest;
import com.fashion.chatbotservice.dto.ChatResponse;
import com.fashion.chatbotservice.dto.SessionResponse;
import com.fashion.chatbotservice.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);

        try {
            log.info("Chat request: sessionId={}, messageLength={}",
                    request.getSessionId(),
                    request.getMessage() != null ? request.getMessage().length() : 0);

            ChatResponse response = chatbotService.chat(request, userId, traceId);
            return ResponseEntity.ok(response);
        } finally {
            MDC.remove("traceId");
        }
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatbotService.getSession(sessionId));
    }
}
