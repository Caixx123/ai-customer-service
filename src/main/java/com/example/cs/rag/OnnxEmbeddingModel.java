package com.example.cs.rag;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.example.cs.config.AppProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Local bge-small-zh-v1.5 embedding: HuggingFace tokenizer -> ONNX Runtime -> CLS pooling -> L2 normalize.
 * Deliberately does not depend on Spring AI; ONNX Runtime is used only as an inference library.
 */
@Component
public class OnnxEmbeddingModel implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OnnxEmbeddingModel.class);

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;
    private final String outputName;
    private final int maxLength;
    private final int dimension;

    public OnnxEmbeddingModel(AppProperties properties, ResourceLoader resourceLoader) {
        try {
            AppProperties.Embedding config = properties.embedding();
            Resource modelResource = resourceLoader.getResource(config.modelPath());
            Resource tokenizerResource = resourceLoader.getResource(config.tokenizerPath());

            this.environment = OrtEnvironment.getEnvironment();
            this.session = environment.createSession(modelResource.getInputStream().readAllBytes());
            this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerResource.getInputStream(), Map.of());
            this.maxLength = config.maxLength() > 0 ? config.maxLength() : 512;
            this.outputName = session.getOutputNames().iterator().next();
            this.dimension = probeDimension();

            log.info("ONNX embedding ready: inputs={}, outputs={}, dimension={}, maxLength={}",
                    session.getInputNames(), session.getOutputNames(), dimension, maxLength);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize local ONNX embedding model. "
                    + "Check app.embedding.model-path and app.embedding.tokenizer-path.", e);
        }
    }

    /**
     * Tool and ingestion threads may call this concurrently; DJL's JNI tokenizer is kept behind a lock
     * until we have a reason to trust its thread-safety in this setup.
     */
    public synchronized float[] embed(String text) {
        String input = (text == null || text.isBlank()) ? " " : text;
        try {
            Encoding encoding = tokenizer.encode(input);
            long[] ids = truncate(encoding.getIds());
            long[] attentionMask = truncate(encoding.getAttentionMask());
            long[] typeIds = truncate(encoding.getTypeIds());
            long[] shape = {1, ids.length};

            Map<String, OnnxTensor> inputs = new HashMap<>();
            try (OnnxTensor idsTensor = OnnxTensor.createTensor(environment, LongBuffer.wrap(ids), shape);
                 OnnxTensor maskTensor = OnnxTensor.createTensor(environment, LongBuffer.wrap(attentionMask), shape);
                 OnnxTensor typeTensor = OnnxTensor.createTensor(environment, LongBuffer.wrap(typeIds), shape)) {
                inputs.put("input_ids", idsTensor);
                inputs.put("attention_mask", maskTensor);
                if (session.getInputNames().contains("token_type_ids")) {
                    inputs.put("token_type_ids", typeTensor);
                }

                try (OrtSession.Result result = session.run(inputs)) {
                    Object value = result.get(outputName)
                            .orElseThrow(() -> new IllegalStateException("Missing ONNX output: " + outputName))
                            .getValue();
                    float[] vector = extractCls(value);
                    normalize(vector);
                    return vector;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to embed text with local ONNX model", e);
        }
    }

    public int dimension() {
        return dimension;
    }

    private int probeDimension() {
        return embed("客服").length;
    }

    private long[] truncate(long[] values) {
        if (values.length <= maxLength) {
            return values;
        }
        long[] truncated = new long[maxLength];
        System.arraycopy(values, 0, truncated, 0, maxLength);
        return truncated;
    }

    private float[] extractCls(Object value) {
        if (value instanceof float[][][] hidden) {
            return hidden[0][0].clone();
        }
        if (value instanceof float[][] hidden) {
            return hidden[0].clone();
        }
        throw new IllegalStateException("Unexpected ONNX output type: " + value.getClass());
    }

    private void normalize(float[] vector) {
        double sum = 0;
        for (float v : vector) {
            sum += (double) v * v;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
    }

    @PreDestroy
    public void close() {
        try {
            session.close();
        } catch (Exception e) {
            log.warn("Failed to close ONNX session", e);
        }
        tokenizer.close();
    }
}
