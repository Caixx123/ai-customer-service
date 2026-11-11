package com.example.cs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Ai ai, Embedding embedding, Kb kb, Agent agent) {

    public record Ai(String baseUrl, String apiKey, String model, int timeoutSeconds) {
    }

    public record Embedding(String modelPath, String tokenizerPath, int maxLength) {
    }

    public record Kb(int chunkSize, int chunkOverlap, int topK, double minScore) {
    }

    public record Agent(String stateDir, int maxIters) {
    }
}
