package com.example.cs.web;

import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single place where AgentScope event classes are translated into the SSE protocol.
 * If the AgentScope SDK changes event types in a 2.0.x update, only this class needs to change.
 */
public final class AgentEventMapper {

    private AgentEventMapper() {
    }

    public static SseMessage map(AgentEvent event) {
        if (event instanceof TextBlockDeltaEvent delta) {
            return new SseMessage("reply", Map.of("delta", delta.getDelta()));
        }
        if (event instanceof ToolCallStartEvent call) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("name", call.getToolCallName());
            payload.put("toolCallId", call.getToolCallId());
            return new SseMessage("tool_call", payload);
        }
        if (event instanceof ToolResultEndEvent result) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("name", result.getToolCallName());
            payload.put("toolCallId", result.getToolCallId());
            payload.put("state", result.getState() == null ? "UNKNOWN" : result.getState().name());
            return new SseMessage("tool_result", payload);
        }
        return null;
    }

    public record SseMessage(String eventName, Object data) {
    }
}
