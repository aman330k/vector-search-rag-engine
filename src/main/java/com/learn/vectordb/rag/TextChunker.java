package com.learn.vectordb.rag;

import com.learn.vectordb.config.VectorDbProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits a long document into smaller overlapping chunks for embedding.
 *
 * <p>Why chunk at all? Embedding models have a limited context window, and retrieval works
 * better when each stored piece is about one idea. Overlap between chunks keeps sentences that
 * straddle a boundary from losing their meaning.
 *
 * <p>Two strategies are provided:
 * <ul>
 *   <li><b>word</b> — fixed-size word windows</li>
 *   <li><b>sentence</b> — packs whole sentences up to a word budget, so chunks never cut a
 *       sentence in half (usually better retrieval quality)</li>
 * </ul>
 */
@Component
public class TextChunker {

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+");

    private final VectorDbProperties.Chunking defaults;

    public TextChunker(VectorDbProperties properties) {
        this.defaults = properties.getChunking();
    }

    /**
     * Chunks text using the configured default strategy and sizes.
     *
     * @param text the document text
     * @return the list of chunks (never null; may contain the whole text as one chunk)
     */
    public List<String> chunk(String text) {
        return chunk(text, defaults.getStrategy(), defaults.getChunkWords(),
                defaults.getOverlapWords());
    }

    /**
     * Chunks text with explicit parameters.
     *
     * @param text         the document text
     * @param strategy     "word" or "sentence"
     * @param chunkWords   target words per chunk
     * @param overlapWords words shared between consecutive chunks (word strategy only)
     * @return the list of chunks
     */
    public List<String> chunk(String text, String strategy, int chunkWords, int overlapWords) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }
        if ("sentence".equalsIgnoreCase(strategy)) {
            return chunkBySentence(text, chunkWords);
        }
        return chunkByWord(text, chunkWords, overlapWords);
    }

    private List<String> chunkByWord(String text, int chunkWords, int overlapWords) {
        String[] words = text.trim().split("\\s+");
        if (words.length == 0) {
            return new ArrayList<>();
        }
        if (words.length <= chunkWords) {
            List<String> single = new ArrayList<>();
            single.add(text.trim());
            return single;
        }
        // Guard against a non-positive step that would loop forever.
        int step = Math.max(1, chunkWords - overlapWords);
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < words.length; i += step) {
            int end = Math.min(i + chunkWords, words.length);
            StringBuilder chunk = new StringBuilder();
            for (int j = i; j < end; j++) {
                if (j > i) {
                    chunk.append(' ');
                }
                chunk.append(words[j]);
            }
            chunks.add(chunk.toString());
            if (end == words.length) {
                break;
            }
        }
        return chunks;
    }

    private List<String> chunkBySentence(String text, int chunkWords) {
        String[] sentences = SENTENCE_BOUNDARY.split(text.trim());
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentWords = 0;

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int words = trimmed.split("\\s+").length;
            if (currentWords > 0 && currentWords + words > chunkWords) {
                chunks.add(current.toString().trim());
                current.setLength(0);
                currentWords = 0;
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(trimmed);
            currentWords += words;
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }
}
