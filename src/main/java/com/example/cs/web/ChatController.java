package com.example.cs.web;

import io.agentscope.core.message.Msg;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(
            @RequestHeader(value = "X-User-Id", defaultValue = "user-001") String userId,
            @Valid @RequestBody ChatRequest request) {
        try {
            Msg reply = chatService.chat(userId, request.sessionId(), request.message());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sessionId", request.sessionId());
            body.put("reply", reply.getTextContent());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Chat request failed: userId={}, sessionId={}", userId, request.sessionId(), e);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sessionId", request.sessionId());
            body.put("error", friendlyMessage(e));
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
        }
    }

    private String friendlyMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return "模型服务暂时不可用，请稍后重试。";
        }
        return "模型服务暂时不可用：" + message.substring(0, Math.min(message.length(), 200));
    }
}
