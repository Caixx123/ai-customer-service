package com.example.cs.rag;

import com.example.cs.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final OnnxEmbeddingModel embeddingModel;
    private final MarkdownChunker chunker;
    private final CosineIndex index;
    private final AppProperties.Kb config;

    public KnowledgeBaseService(OnnxEmbeddingModel embeddingModel, MarkdownChunker chunker,
                                CosineIndex index, AppProperties properties) {
        this.embeddingModel = embeddingModel;
        this.chunker = chunker;
        this.index = index;
        this.config = properties.kb();
    }

    public synchronized void ingest() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:knowledge/*.md");
            Arrays.sort(resources, Comparator.comparing(r -> r.getFilename() == null ? "" : r.getFilename()));

            index.clear();
            int chunks = 0;
            for (Resource resource : resources) {
                String source = resource.getFilename() == null ? "unknown" : resource.getFilename();
                String markdown = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                List<KnowledgeChunk> parsed = chunker.chunk(source, markdown,
                        config.chunkSize(), config.chunkOverlap());
                for (KnowledgeChunk chunk : parsed) {
                    float[] vector = embeddingModel.embed(chunk.section() + "\n" + chunk.text());
                    index.add(chunk, vector);
                    chunks++;
                }
            }
            log.info("Knowledge ingestion finished: {} documents, {} chunks, dimension={}",
                    resources.length, chunks, embeddingModel.dimension());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to ingest knowledge base", e);
        }
    }

    public List<CosineIndex.SearchHit> search(String query) {
        float[] vector = embeddingModel.embed(query);
        return index.search(vector, config.topK(), config.minScore());
    }

    public int size() {
        return index.size();
    }
}
