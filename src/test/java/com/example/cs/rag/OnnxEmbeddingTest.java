package com.example.cs.rag;

import com.example.cs.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnnxEmbeddingTest {

    @Test
    void loadsLocalModelAndProducesSemanticVectors() {
        AppProperties properties = new AppProperties(
                new AppProperties.Ai("", "", "", 90),
                new AppProperties.Embedding("classpath:/onnx/model_quantized.onnx",
                        "classpath:/onnx/tokenizer.json", 512),
                new AppProperties.Kb(300, 50, 4, 0.3),
                new AppProperties.Agent("./data/test-state", 8));

        try (OnnxEmbeddingModel model = new OnnxEmbeddingModel(properties, new DefaultResourceLoader())) {
            float[] query = model.embed("退款多久到账");
            float[] refund = model.embed("退货退款政策：退款审核通过后，款项将退回原支付账户");
            float[] invoice = model.embed("发票开具：支持电子发票，可在订单详情页申请");

            assertEquals(512, query.length);
            assertTrue(Math.abs(norm(query) - 1.0) < 1e-4, "embedding should be L2 normalized");
            assertTrue(cosine(query, refund) > cosine(query, invoice),
                    "refund query should be closer to refund policy than to invoice policy");
        }
    }

    private double cosine(float[] a, float[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private double norm(float[] v) {
        double sum = 0;
        for (float x : v) {
            sum += x * x;
        }
        return Math.sqrt(sum);
    }
}
