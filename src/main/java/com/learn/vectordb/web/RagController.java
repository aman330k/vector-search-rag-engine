package com.learn.vectordb.web;

import com.learn.vectordb.embedding.OllamaClient;
import com.learn.vectordb.model.RagResponse;
import com.learn.vectordb.rag.RagService;
import com.learn.vectordb.store.DocumentStore;
import com.learn.vectordb.store.VectorStore;
import com.learn.vectordb.web.dto.AskRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST endpoints for the RAG pipeline and server status.
 *
 * <p>Handles {@code /doc/ask} and {@code /status}.
 */
@RestController
public class RagController {

    private final RagService ragService;
    private final OllamaClient ollama;
    private final DocumentStore documentStore;
    private final VectorStore vectorStore;

    public RagController(RagService ragService, OllamaClient ollama,
                         DocumentStore documentStore, VectorStore vectorStore) {
        this.ragService = ragService;
        this.ollama = ollama;
        this.documentStore = documentStore;
        this.vectorStore = vectorStore;
    }

    @PostMapping("/doc/ask")
    public ResponseEntity<?> ask(@RequestBody AskRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.ok(Map.of("error", "need question"));
        }
        try {
            RagResponse response = ragService.ask(request.question(), request.effectiveK());
            return ResponseEntity.ok(response);
        } catch (RagService.OllamaUnavailableException e) {
            return ResponseEntity.ok(Map.of("error", "Ollama unavailable"));
        }
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ollamaAvailable", ollama.isAvailable());
        m.put("embedModel", ollama.getEmbedModel());
        m.put("genModel", ollama.getGenModel());
        m.put("docCount", documentStore.size());
        m.put("docDims", documentStore.getDims());
        m.put("demoDims", VectorStore.DIMS);
        m.put("demoCount", vectorStore.size());
        return m;
    }
}
