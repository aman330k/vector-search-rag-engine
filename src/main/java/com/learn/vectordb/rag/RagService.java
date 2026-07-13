package com.learn.vectordb.rag;

import com.learn.vectordb.embedding.OllamaClient;
import com.learn.vectordb.model.DocChunk;
import com.learn.vectordb.model.RagContext;
import com.learn.vectordb.model.RagResponse;
import com.learn.vectordb.store.DocumentStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the full RAG (Retrieval-Augmented Generation) pipeline.
 *
 * <p>The steps are:
 * <ol>
 *   <li>Embed the user's question with the embedding model.</li>
 *   <li>Retrieve the top-k most similar chunks from the {@link DocumentStore}.</li>
 *   <li>Build a prompt that stuffs those chunks in as context.</li>
 *   <li>Ask the LLM to generate an answer.</li>
 *   <li>Return the answer plus the contexts that were used (for transparency).</li>
 * </ol>
 */
@Service
public class RagService {

    private final OllamaClient ollama;
    private final DocumentStore documentStore;

    public RagService(OllamaClient ollama, DocumentStore documentStore) {
        this.ollama = ollama;
        this.documentStore = documentStore;
    }

    /** Signals that Ollama was unreachable, so the controller can return a helpful error. */
    public static class OllamaUnavailableException extends RuntimeException {
        public OllamaUnavailableException(String message) {
            super(message);
        }
    }

    /**
     * Retrieval only (no generation) — used by the UI to preview which chunks match.
     *
     * @param question the user's question
     * @param k        number of chunks to retrieve
     * @return the matching contexts, closest first
     */
    public List<RagContext> retrieve(String question, int k) {
        float[] queryEmbedding = ollama.embed(question);
        if (queryEmbedding.length == 0) {
            throw new OllamaUnavailableException("Ollama unavailable");
        }
        List<Map.Entry<Float, DocChunk>> hits = documentStore.search(queryEmbedding, k);
        List<RagContext> contexts = new ArrayList<>();
        for (Map.Entry<Float, DocChunk> hit : hits) {
            DocChunk chunk = hit.getValue();
            contexts.add(new RagContext(chunk.id(), chunk.title(), chunk.text(), hit.getKey()));
        }
        return contexts;
    }

    /**
     * Full pipeline: retrieve relevant chunks and have the LLM answer using them.
     *
     * @param question the user's question
     * @param k        number of chunks to retrieve as context
     * @return the generated answer plus supporting contexts
     */
    public RagResponse ask(String question, int k) {
        float[] queryEmbedding = ollama.embed(question);
        if (queryEmbedding.length == 0) {
            throw new OllamaUnavailableException("Ollama unavailable");
        }

        List<Map.Entry<Float, DocChunk>> hits = documentStore.search(queryEmbedding, k);

        StringBuilder context = new StringBuilder();
        List<RagContext> contexts = new ArrayList<>();
        int i = 1;
        for (Map.Entry<Float, DocChunk> hit : hits) {
            DocChunk chunk = hit.getValue();
            context.append('[').append(i++).append("] ")
                    .append(chunk.title()).append(":\n")
                    .append(chunk.text()).append("\n\n");
            contexts.add(new RagContext(chunk.id(), chunk.title(), chunk.text(), hit.getKey()));
        }

        String prompt = buildPrompt(context.toString(), question);
        String answer = ollama.generate(prompt);

        return new RagResponse(answer, ollama.getGenModel(), contexts, documentStore.size());
    }

    private String buildPrompt(String context, String question) {
        return "You are a helpful assistant. Answer the user's question directly. "
                + "Use the provided context if it contains relevant information. "
                + "If it doesn't, just use your own general knowledge. "
                + "IMPORTANT: Do NOT mention the 'context', 'provided text', or say things like "
                + "'the context doesn't mention'. Just answer the question naturally.\n\n"
                + "Context:\n" + context
                + "Question: " + question + "\n\n"
                + "Answer:";
    }
}
