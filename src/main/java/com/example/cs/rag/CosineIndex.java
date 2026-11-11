package com.example.cs.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class CosineIndex {

    private final List<Entry> entries = new ArrayList<>();

    public synchronized void add(KnowledgeChunk chunk, float[] vector) {
        entries.add(new Entry(chunk, vector));
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized List<SearchHit> search(float[] query, int topK, double minScore) {
        return entries.stream()
                .map(entry -> new SearchHit(entry.chunk(), dot(query, entry.vector())))
                .filter(hit -> hit.score() >= minScore)
                .sorted(Comparator.comparingDouble(SearchHit::score).reversed())
                .limit(topK)
                .toList();
    }

    private double dot(float[] a, float[] b) {
        int length = Math.min(a.length, b.length);
        double sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (double) a[i] * b[i];
        }
        return sum;
    }

    private record Entry(KnowledgeChunk chunk, float[] vector) {
    }

    public record SearchHit(KnowledgeChunk chunk, double score) {
    }
}
