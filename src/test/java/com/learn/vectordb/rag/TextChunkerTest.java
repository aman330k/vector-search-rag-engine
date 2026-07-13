package com.learn.vectordb.rag;

import com.learn.vectordb.config.VectorDbProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker(new VectorDbProperties());

    private String words(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append("w").append(i);
        }
        return sb.toString();
    }

    @Test
    void blankTextReturnsEmptyList() {
        assertTrue(chunker.chunk("").isEmpty());
        assertTrue(chunker.chunk("   ").isEmpty());
        assertTrue(chunker.chunk(null).isEmpty());
    }

    @Test
    void shortTextIsASingleChunk() {
        String text = words(50);
        List<String> chunks = chunker.chunk(text, "word", 250, 30);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    void longTextIsSplitIntoOverlappingChunks() {
        // 500 words, chunk=100, overlap=20 -> step 80 -> chunks at 0,80,160,...
        List<String> chunks = chunker.chunk(words(500), "word", 100, 20);
        assertTrue(chunks.size() > 1, "expected multiple chunks");
        // Overlap: last 20 words of chunk 0 should reappear at the start of chunk 1.
        String[] first = chunks.get(0).split("\\s+");
        String[] second = chunks.get(1).split("\\s+");
        assertEquals(first[80], second[0], "chunks should overlap by 20 words");
    }

    @Test
    void wordChunkerDoesNotLoopWhenOverlapExceedsChunk() {
        // overlap >= chunk would make step <= 0; the chunker guards against an infinite loop.
        List<String> chunks = chunker.chunk(words(300), "word", 50, 100);
        assertTrue(chunks.size() >= 1);
    }

    @Test
    void sentenceStrategyDoesNotCutSentences() {
        String text = "First sentence here. Second sentence follows. Third one ends it.";
        List<String> chunks = chunker.chunk(text, "sentence", 5, 0);
        // Every chunk must end with sentence punctuation (never a mid-sentence break).
        for (String chunk : chunks) {
            char last = chunk.charAt(chunk.length() - 1);
            assertTrue(last == '.' || last == '!' || last == '?',
                    "chunk should end at a sentence boundary: " + chunk);
        }
    }
}
