package com.example.cs.agent.tools;

import com.example.cs.domain.LogisticsEntity;
import com.example.cs.domain.OrderEntity;
import com.example.cs.domain.RefundEntity;
import com.example.cs.domain.repository.LogisticsRepository;
import com.example.cs.domain.repository.OrderRepository;
import com.example.cs.domain.repository.RefundRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderTools {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String NOT_FOUND = "未找到该订单，或该订单不属于当前用户";

    private final OrderRepository orderRepository;
    private final LogisticsRepository logisticsRepository;
    private final RefundRepository refundRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderTools(OrderRepository orderRepository, LogisticsRepository logisticsRepository,
                      RefundRepository refundRepository) {
        this.orderRepository = orderRepository;
        this.logisticsRepository = logisticsRepository;
        this.refundRepository = refundRepository;
    }

    @Tool(name = "list_my_orders", description = "查询当前登录用户最近的订单列表，最多返回 5 笔。")
    public String listMyOrders(RuntimeContext ctx) {
        List<OrderEntity> orders = orderRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId(ctx));
        List<Map<String, Object>> items = orders.stream().map(this::toOrderSummary).toList();
        return json(Map.of("orders", items, "count", items.size()));
    }

    @Tool(name = "query_order", description = "按订单号查询当前登录用户的订单详情，包括商品、金额和订单状态。")
    public String queryOrder(
            @ToolParam(name = "orderNo", description = "订单号，例如 O20261023001") String orderNo,
            RuntimeContext ctx) {
        return orderRepository.findByOrderNoAndUserId(orderNo, userId(ctx))
                .map(order -> json(toOrderDetail(order)))
                .orElseGet(() -> notFound());
    }

    @Tool(name = "query_logistics", description = "按订单号查询物流信息，包括承运商、运单号、最新状态和轨迹。")
    public String queryLogistics(
            @ToolParam(name = "orderNo", description = "订单号，例如 O20261023003") String orderNo,
            RuntimeContext ctx) {
        if (orderRepository.findByOrderNoAndUserId(orderNo, userId(ctx)).isEmpty()) {
            return notFound();
        }
        return logisticsRepository.findByOrderNo(orderNo)
                .map(logistics -> json(toLogistics(logistics)))
                .orElseGet(() -> json(Map.of("found", false, "message", "该订单暂无物流信息")));
    }

    @Tool(name = "query_refund", description = "按订单号查询退款进度，包括退款金额、状态、原因和时间。")
    public String queryRefund(
            @ToolParam(name = "orderNo", description = "订单号，例如 O20261023002") String orderNo,
            RuntimeContext ctx) {
        if (orderRepository.findByOrderNoAndUserId(orderNo, userId(ctx)).isEmpty()) {
            return notFound();
        }
        return refundRepository.findByOrderNoAndUserId(orderNo, userId(ctx))
                .map(refund -> json(toRefund(refund)))
                .orElseGet(() -> json(Map.of("found", false, "message", "该订单暂无退款记录")));
    }

    private String userId(RuntimeContext ctx) {
        String userId = ctx == null ? null : ctx.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Missing user identity in RuntimeContext");
        }
        return userId;
    }

    private Map<String, Object> toOrderSummary(OrderEntity order) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("orderNo", order.getOrderNo());
        item.put("productName", order.getProductName());
        item.put("amount", order.getAmount());
        item.put("status", order.getStatus().name());
        item.put("statusLabel", order.getStatus().label());
        item.put("createdAt", order.getCreatedAt().format(TIME_FORMAT));
        return item;
    }

    private Map<String, Object> toOrderDetail(OrderEntity order) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("found", true);
        item.putAll(toOrderSummary(order));
        return item;
    }

    private Map<String, Object> toLogistics(LogisticsEntity logistics) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("found", true);
        item.put("orderNo", logistics.getOrderNo());
        item.put("carrier", logistics.getCarrier());
        item.put("trackingNo", logistics.getTrackingNo());
        item.put("status", logistics.getStatus());
        item.put("detail", logistics.getDetail());
        item.put("updatedAt", logistics.getUpdatedAt().format(TIME_FORMAT));
        return item;
    }

    private Map<String, Object> toRefund(RefundEntity refund) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("found", true);
        item.put("orderNo", refund.getOrderNo());
        item.put("amount", refund.getAmount());
        item.put("status", refund.getStatus().name());
        item.put("statusLabel", refund.getStatus().label());
        item.put("reason", refund.getReason());
        item.put("appliedAt", refund.getAppliedAt().format(TIME_FORMAT));
        item.put("updatedAt", refund.getUpdatedAt().format(TIME_FORMAT));
        return item;
    }

    private String notFound() {
        return json(Map.of("found", false, "message", NOT_FOUND));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize tool result", e);
        }
    }
}
