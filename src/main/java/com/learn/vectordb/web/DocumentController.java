package com.learn.vectordb.web;

import com.learn.vectordb.model.DocChunk;
import com.learn.vectordb.model.RagContext;
import com.learn.vectordb.rag.DocumentIngestService;
import com.learn.vectordb.rag.RagService;
import com.learn.vectordb.store.DocumentStore;
import com.learn.vectordb.web.dto.AskRequest;
import com.learn.vectordb.web.dto.InsertDocumentRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for document ingestion and retrieval.
 *
 * <p>Handles {@code /doc/insert}, {@code /doc/list}, {@code /doc/delete}, and
 * {@code /doc/search}.
 */
@RestController
public class DocumentController {

    private static final int PREVIEW_CHARS = 120;

    private final DocumentIngestService ingestService;
    private final DocumentStore documentStore;
    private final RagService ragService;

    public DocumentController(DocumentIngestService ingestService, DocumentStore documentStore,
                              RagService ragService) {
        this.ingestService = ingestService;
        this.documentStore = documentStore;
        this.ragService = ragService;
    }

    @PostMapping("/doc/insert")
    public ResponseEntity<?> insert(@RequestBody InsertDocumentRequest request) {
        if (request.title() == null || request.title().isBlank()
                || request.text() == null || request.text().isBlank()) {
            return ResponseEntity.ok(Map.of("error", "need title and text"));
        }
        try {
            DocumentIngestService.IngestResult result =
                    ingestService.ingest(request.title(), request.text());
            return ResponseEntity.ok(Map.of(
                    "ids", result.ids(),
                    "chunks", result.chunks(),
                    "dims", result.dims()));
        } catch (RagService.OllamaUnavailableException e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/doc/list")
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (DocChunk chunk : documentStore.all()) {
            String text = chunk.text();
            String preview = text.length() > PREVIEW_CHARS
                    ? text.substring(0, PREVIEW_CHARS) + "\u2026"
                    : text;
            int words = text.isBlank() ? 0 : text.trim().split("\\s+").length;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", chunk.id());
            m.put("title", chunk.title());
            m.put("preview", preview);
            m.put("words", words);
            out.add(m);
        }
        return out;
    }

    @DeleteMapping("/doc/delete/{id}")
    public Map<String, Object> delete(@PathVariable int id) {
        return Map.of("ok", documentStore.remove(id));
    }

    @PostMapping("/doc/search")
    public ResponseEntity<?> search(@RequestBody AskRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.ok(Map.of("error", "need question"));
        }
        try {
            List<RagContext> contexts = ragService.retrieve(request.question(), request.effectiveK());
            List<Map<String, Object>> simplified = new ArrayList<>();
            for (RagContext ctx : contexts) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", ctx.id());
                m.put("title", ctx.title());
                m.put("distance", ctx.distance());
                simplified.add(m);
            }
            return ResponseEntity.ok(Map.of("contexts", simplified));
        } catch (RagService.OllamaUnavailableException e) {
            return ResponseEntity.ok(Map.of("error", "Ollama unavailable"));
        }
    }
}
