package com.learn.vectordb.rag;

import com.learn.vectordb.embedding.OllamaClient;
import com.learn.vectordb.store.DocumentStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Ingests a raw document: chunk it, embed each chunk via Ollama, and store the results.
 *
 * <p>Kept separate from the controller so it can be unit-tested with a mocked embedding client.
 */
@Service
public class DocumentIngestService {

    private final TextChunker chunker;
    private final OllamaClient ollama;
    private final DocumentStore documentStore;

    public DocumentIngestService(TextChunker chunker, OllamaClient ollama,
                                 DocumentStore documentStore) {
        this.chunker = chunker;
        this.ollama = ollama;
        this.documentStore = documentStore;
    }

    /** Result of ingesting one document. */
    public record IngestResult(List<Integer> ids, int chunks, int dims) {
    }

    /**
     * Chunks and embeds a document, storing every chunk.
     *
     * @param title document title
     * @param text  document body
     * @return the ids created, the number of chunks, and the embedding dimensionality
     * @throws RagService.OllamaUnavailableException if embedding fails (Ollama offline)
     */
    public IngestResult ingest(String title, String text) {
        List<String> chunks = chunker.chunk(text);
        List<Integer> ids = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            float[] embedding = ollama.embed(chunks.get(i));
            if (embedding.length == 0) {
                throw new RagService.OllamaUnavailableException(
                        "Ollama unavailable. Install from https://ollama.com then run: "
                                + "ollama pull nomic-embed-text && ollama pull llama3.2");
            }
            String chunkTitle = chunks.size() > 1
                    ? title + " [" + (i + 1) + "/" + chunks.size() + "]"
                    : title;
            ids.add(documentStore.insert(chunkTitle, chunks.get(i), embedding));
        }

        return new IngestResult(ids, chunks.size(), documentStore.getDims());
    }
}
