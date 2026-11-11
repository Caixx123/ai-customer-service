package com.example.cs.agent.tools;

import io.agentscope.core.agent.RuntimeContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OrderToolsTest {

    @Autowired
    private OrderTools orderTools;

    private final RuntimeContext userOne = RuntimeContext.builder()
            .userId("user-001")
            .sessionId("test-session")
            .build();

    @Test
    void listsOnlyCurrentUsersOrders() {
        String result = orderTools.listMyOrders(userOne);
        assertTrue(result.contains("O20261023001"));
        assertFalse(result.contains("O20261023004"), "must not leak user-002 orders");
    }

    @Test
    void rejectsCrossUserOrderLookup() {
        String result = orderTools.queryOrder("O20261023004", userOne);
        assertTrue(result.contains("\"found\":false"));
        assertFalse(result.contains("27寸显示器"));
    }

    @Test
    void returnsLogisticsAndRefundForOwnedOrder() {
        assertTrue(orderTools.queryLogistics("O20261023003", userOne).contains("运输中"));
        assertTrue(orderTools.queryRefund("O20261023002", userOne).contains("商家审核中"));
    }
}
