package com.example.cs.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class KnowledgeRetrievalTest {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Test
    void knowledgeBaseIsIngested() {
        assertTrue(knowledgeBaseService.size() > 0, "knowledge base should contain chunks");
    }

    @Test
    void sevenDayReturnQuestionHitsRefundPolicy() {
        List<CosineIndex.SearchHit> hits = knowledgeBaseService.search("7天无理由怎么算");
        assertFalse(hits.isEmpty(), "expected at least one knowledge hit");
        assertTrue(hits.stream().anyMatch(hit -> hit.chunk().source().contains("退货退款政策")),
                "expected refund policy in hits, got: " + hits);
    }

    @Test
    void priceProtectionQuestionHitsPricePolicy() {
        List<CosineIndex.SearchHit> hits = knowledgeBaseService.search("买完就降价了，可以价保吗");
        assertFalse(hits.isEmpty(), "expected at least one knowledge hit");
        assertTrue(hits.stream().anyMatch(hit -> hit.chunk().source().contains("价保规则")),
                "expected price protection policy in hits, got: " + hits);
    }
}
