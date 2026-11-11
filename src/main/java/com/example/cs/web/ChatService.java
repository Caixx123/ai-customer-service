package com.example.cs.web;

import com.example.cs.config.AppProperties;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ReActAgent supervisorAgent;
    private final AppProperties properties;

    public ChatService(@Qualifier("supervisorAgent") ReActAgent supervisorAgent, AppProperties properties) {
        this.supervisorAgent = supervisorAgent;
        this.properties = properties;
    }

    public Msg chat(String userId, String sessionId, String message) {
        RuntimeContext context = context(userId, sessionId);
        log.info("Chat request: userId={}, sessionId={}, message={}", userId, sessionId, abbreviate(message));
        Msg reply = supervisorAgent.call(java.util.List.of(new UserMessage(message)), context)
                .block(Duration.ofSeconds(properties.ai().timeoutSeconds()));
        if (reply == null) {
            throw new IllegalStateException("Agent returned no reply");
        }
        return reply;
    }

    public Flux<AgentEvent> stream(String userId, String sessionId, String message) {
        RuntimeContext context = context(userId, sessionId);
        log.info("Streaming chat request: userId={}, sessionId={}, message={}",
                userId, sessionId, abbreviate(message));
        return supervisorAgent.streamEvents(new UserMessage(message), context);
    }

    private RuntimeContext context(String userId, String sessionId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Missing X-User-Id header");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Missing sessionId");
        }
        return RuntimeContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 120 ? value : value.substring(0, 120) + "...";
    }
}
