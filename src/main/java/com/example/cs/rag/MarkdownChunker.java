package com.example.cs.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MarkdownChunker {

    public List<KnowledgeChunk> chunk(String source, String markdown, int chunkSize, int overlap) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String[] lines = markdown.split("\\R");
        String title = source.replaceFirst("\\.md$", "");
        String section = title;
        StringBuilder body = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                if (title.isBlank()) {
                    title = trimmed.substring(2).trim();
                    section = title;
                }
                continue;
            }
            if (trimmed.startsWith("## ") || trimmed.startsWith("### ")) {
                flush(source, section, body, chunkSize, overlap, chunks);
                section = trimmed.replaceFirst("^#+\\s*", "").trim();
                continue;
            }
            if (!trimmed.isEmpty()) {
                body.append(trimmed).append('\n');
            }
        }
        flush(source, section, body, chunkSize, overlap, chunks);
        return chunks;
    }

    private void flush(String source, String section, StringBuilder body,
                       int chunkSize, int overlap, List<KnowledgeChunk> chunks) {
        String text = body.toString().trim();
        body.setLength(0);
        if (text.isEmpty()) {
            return;
        }
        if (text.length() <= chunkSize) {
            chunks.add(new KnowledgeChunk(source, section, text));
            return;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + chunkSize);
            if (end < text.length()) {
                end = findCut(text, start, end);
            }
            String piece = text.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(new KnowledgeChunk(source, section, piece));
            }
            if (end >= text.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
    }

    private int findCut(String text, int start, int end) {
        int floor = Math.max(start + 1, end - 80);
        for (int i = end - 1; i >= floor; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '；' || c == '\n') {
                return i + 1;
            }
        }
        return end;
    }
}
