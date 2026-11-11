package com.example.cs.agent.tools;

import com.example.cs.rag.CosineIndex;
import com.example.cs.rag.KnowledgeBaseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class KnowledgeTools {

    private final KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeTools(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Tool(name = "search_knowledge_base",
            description = "检索电商售后政策与FAQ知识库。适用于退换货、退款时效、价保、运费、物流、发票、会员权益等政策类问题。")
    public String searchKnowledgeBase(
            @ToolParam(name = "query", description = "用户问题的检索语句，尽量保留关键词") String query) {
        List<CosineIndex.SearchHit> hits = knowledgeBaseService.search(query);
        if (hits.isEmpty()) {
            return "未检索到相关资料";
        }

        List<Map<String, Object>> payloadHits = hits.stream().map(hit -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("source", hit.chunk().source());
            item.put("section", hit.chunk().section());
            item.put("score", Math.round(hit.score() * 1000.0) / 1000.0);
            item.put("text", hit.chunk().text());
            return item;
        }).toList();

        StringBuilder context = new StringBuilder();
        for (CosineIndex.SearchHit hit : hits) {
            context.append("【来源：").append(hit.chunk().source())
                    .append(" · ").append(hit.chunk().section()).append("】\n")
                    .append(hit.chunk().text()).append("\n\n");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hits", payloadHits);
        payload.put("context", context.toString().trim());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize knowledge search result", e);
        }
    }
}
