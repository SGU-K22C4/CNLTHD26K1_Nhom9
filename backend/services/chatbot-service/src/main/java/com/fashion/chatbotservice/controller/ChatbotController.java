package com.fashion.chatbotservice.controller;

import com.fashion.chatbotservice.service.ChatbotService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/chat")
    public Mono<ResponseEntity<Map<String, String>>> chat(@RequestBody ChatRequest request) {
        return chatbotService.chat(request.getMessage(), request.getHistory())
                .map(reply -> ResponseEntity.ok(Map.of("reply", reply)));
    }

    @Data
    public static class ChatRequest {
        private String message;
        private List<Map<String, String>> history;
    }
}
