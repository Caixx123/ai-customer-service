package com.example.cs.domain;

import com.example.cs.domain.repository.LogisticsRepository;
import com.example.cs.domain.repository.OrderRepository;
import com.example.cs.domain.repository.RefundRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OrderAccessTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private LogisticsRepository logisticsRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Test
    void orderQueriesAreScopedToCurrentUser() {
        assertTrue(orderRepository.findByOrderNoAndUserId("O20261023001", "user-001").isPresent());
        assertTrue(orderRepository.findByOrderNoAndUserId("O20261023001", "user-002").isEmpty());
    }

    @Test
    void recentOrdersOnlyContainCurrentUser() {
        List<OrderEntity> orders = orderRepository.findTop5ByUserIdOrderByCreatedAtDesc("user-001");
        assertEquals(4, orders.size());
        assertTrue(orders.stream().allMatch(order -> "user-001".equals(order.getUserId())));
    }

    @Test
    void logisticsAndRefundLookupsAreOwnedByUser() {
        assertTrue(logisticsRepository.findByOrderNo("O20261023003").isPresent());
        assertTrue(refundRepository.findByOrderNoAndUserId("O20261023002", "user-001").isPresent());
        assertTrue(refundRepository.findByOrderNoAndUserId("O20261023002", "user-002").isEmpty());
    }
}
