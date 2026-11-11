package com.example.cs.rag;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeIngestionRunner implements ApplicationRunner {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeIngestionRunner(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Override
    public void run(ApplicationArguments args) {
        knowledgeBaseService.ingest();
    }
}
