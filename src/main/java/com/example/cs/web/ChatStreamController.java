package com.example.cs.web;

import io.agentscope.core.event.AgentEvent;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/chat")
public class ChatStreamController {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamController.class);
    private static final long SSE_TIMEOUT_MILLIS = 120_000L;

    private final ChatService chatService;

    public ChatStreamController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestHeader(value = "X-User-Id", defaultValue = "user-001") String userId,
            @Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        AtomicReference<Disposable> subscription = new AtomicReference<>();

        Runnable dispose = () -> {
            Disposable current = subscription.getAndSet(null);
            if (current != null && !current.isDisposed()) {
                current.dispose();
            }
        };
        emitter.onCompletion(dispose);
        emitter.onTimeout(() -> {
            dispose.run();
            emitter.complete();
        });
        emitter.onError(error -> dispose.run());

        send(emitter, "agent_start", Map.of("sessionId", request.sessionId()));

        try {
            Disposable disposable = chatService.stream(userId, request.sessionId(), request.message())
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe(
                            event -> handleEvent(emitter, dispose, event),
                            error -> {
                                log.error("SSE chat failed: userId={}, sessionId={}", userId,
                                        request.sessionId(), error);
                                send(emitter, "error", Map.of("message", friendlyMessage(error)));
                                emitter.complete();
                            },
                            () -> {
                                send(emitter, "done", Map.of("sessionId", request.sessionId()));
                                emitter.complete();
                            });
            subscription.set(disposable);
        } catch (Exception e) {
            log.error("Failed to start SSE chat: userId={}, sessionId={}", userId, request.sessionId(), e);
            send(emitter, "error", Map.of("message", friendlyMessage(e)));
            emitter.complete();
        }
        return emitter;
    }

    private void handleEvent(SseEmitter emitter, Runnable dispose, AgentEvent event) {
        AgentEventMapper.SseMessage message = AgentEventMapper.map(event);
        if (message == null) {
            return;
        }
        if (!send(emitter, message.eventName(), message.data())) {
            dispose.run();
            emitter.completeWithError(new IOException("SSE client disconnected"));
        }
    }

    private boolean send(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
            return true;
        } catch (IOException | IllegalStateException e) {
            log.debug("Failed to send SSE event {}", eventName, e);
            return false;
        }
    }

    private String friendlyMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            return "模型服务暂时不可用，请稍后重试。";
        }
        return "模型服务暂时不可用：" + message.substring(0, Math.min(message.length(), 200));
    }
}
