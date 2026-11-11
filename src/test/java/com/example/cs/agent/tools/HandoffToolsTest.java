package com.example.cs.agent.tools;

import com.example.cs.domain.repository.TicketRepository;
import io.agentscope.core.agent.RuntimeContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class HandoffToolsTest {

    @Autowired
    private HandoffTools handoffTools;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void createsOpenTicketForCurrentUser() {
        long before = ticketRepository.count();
        RuntimeContext context = RuntimeContext.builder()
                .userId("user-001")
                .sessionId("handoff-test")
                .build();

        String result = handoffTools.handoffToHuman("投诉", "商品有质量问题，要求人工处理", context);

        assertTrue(result.contains("\"success\":true"));
        assertTrue(result.contains("ticketNo"));
        assertEquals(before + 1, ticketRepository.count());
    }
}
