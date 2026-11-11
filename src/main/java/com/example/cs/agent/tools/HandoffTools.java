package com.example.cs.agent.tools;

import com.example.cs.service.TicketService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HandoffTools {

    private final TicketService ticketService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HandoffTools(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Tool(name = "handoff_to_human",
            description = "当用户投诉、情绪激动、要求赔偿、明确要求人工客服，或问题无法解决时，创建转人工工单。")
    public String handoffToHuman(
            @ToolParam(name = "reason", description = "转人工原因，例如投诉、赔偿争议、要求人工") String reason,
            @ToolParam(name = "summary", description = "问题摘要，包含用户诉求和已查明的关键信息") String summary,
            RuntimeContext ctx) {
        String userId = ctx == null ? null : ctx.getUserId();
        String sessionId = ctx == null ? null : ctx.getSessionId();
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Missing user identity in RuntimeContext");
        }
        String ticketNo = ticketService.create(userId, sessionId == null ? "unknown" : sessionId, reason, summary);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("ticketNo", ticketNo);
        payload.put("message", "已为你创建转人工工单，客服将在 24 小时内联系你。");
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize handoff result", e);
        }
    }
}
