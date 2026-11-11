package com.example.cs.agent.tools;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class ExpertDelegateTools {

    private static final Logger log = LoggerFactory.getLogger(ExpertDelegateTools.class);

    private final ReActAgent orderExpert;
    private final ReActAgent policyExpert;

    public ExpertDelegateTools(ReActAgent orderExpert, ReActAgent policyExpert) {
        this.orderExpert = orderExpert;
        this.policyExpert = policyExpert;
    }

    @Tool(name = "order_expert",
            description = "处理订单、物流、退款进度查询。输入应是一个自包含的问题，例如“查询用户最近订单”或“订单 O20261023003 的物流状态”。")
    public Mono<String> askOrderExpert(
            @ToolParam(name = "query", description = "自包含的订单相关问题，带上订单号等信息") String query,
            RuntimeContext ctx) {
        return delegate(orderExpert, "order_expert", query, ctx);
    }

    @Tool(name = "policy_expert",
            description = "处理售后政策、退换货规则、价保、运费、发票、会员权益等 FAQ 咨询。输入应是一个自包含的问题。")
    public Mono<String> askPolicyExpert(
            @ToolParam(name = "query", description = "自包含的政策类问题") String query,
            RuntimeContext ctx) {
        return delegate(policyExpert, "policy_expert", query, ctx);
    }

    private Mono<String> delegate(ReActAgent expert, String name, String query, RuntimeContext ctx) {
        String userId = ctx == null ? null : ctx.getUserId();
        String sessionId = ctx == null ? null : ctx.getSessionId();
        if (userId == null || userId.isBlank()) {
            return Mono.error(new IllegalStateException("Missing user identity in RuntimeContext"));
        }
        log.info("Delegating to {}: userId={}, query={}", name, userId, abbreviate(query));

        // Experts are stateless: give them a fresh context with the same identity, but not the
        // supervisor's AgentState, so their per-call state cannot mix with the main conversation.
        RuntimeContext expertContext = RuntimeContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
        return expert.call(java.util.List.of(new UserMessage(query)), expertContext)
                .map(Msg::getTextContent)
                .doOnNext(answer -> log.info("{} answered {} chars", name, answer == null ? 0 : answer.length()));
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 120 ? value : value.substring(0, 120) + "...";
    }
}
