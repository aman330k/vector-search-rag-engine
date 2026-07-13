package com.learn.vectordb.web;

import com.learn.vectordb.distance.DistanceMetric;
import com.learn.vectordb.index.HnswIndex;
import com.learn.vectordb.model.VectorItem;
import com.learn.vectordb.store.BenchmarkResult;
import com.learn.vectordb.store.VectorStore;
import com.learn.vectordb.web.dto.InsertVectorRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for the 16-D demo vector database.
 *
 * <p>Endpoints for the 16-D demo vectors: {@code /search}, {@code /insert}, {@code /delete},
 * {@code /items}, {@code /benchmark}, {@code /hnsw-info}, {@code /stats}. Adds an optional
 * {@code category} query parameter to {@code /search} for metadata filtering.
 */
@RestController
public class SearchController {

    private final VectorStore vectorStore;

    public SearchController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam("v") String v,
            @RequestParam(value = "k", defaultValue = "5") int k,
            @RequestParam(value = "metric", defaultValue = "cosine") String metric,
            @RequestParam(value = "algo", defaultValue = "hnsw") String algo,
            @RequestParam(value = "category", required = false) String category) {
        float[] query = parseVector(v);
        if (query.length != VectorStore.DIMS) {
            return ResponseEntity.ok(Map.of("error", "need " + VectorStore.DIMS + "D vector"));
        }
        return ResponseEntity.ok(vectorStore.search(query, k, metric, algo, category));
    }

    @PostMapping("/insert")
    public ResponseEntity<?> insert(@RequestBody InsertVectorRequest request) {
        if (request.metadata() == null || request.metadata().isBlank()
                || request.embedding() == null || request.embedding().length != VectorStore.DIMS) {
            return ResponseEntity.ok(Map.of("error", "invalid body"));
        }
        String category = request.category() == null ? "" : request.category();
        int id = vectorStore.insert(request.metadata(), category, request.embedding(),
                DistanceMetric.COSINE);
        return ResponseEntity.ok(Map.of("id", id));
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable int id) {
        return Map.of("ok", vectorStore.remove(id));
    }

    @GetMapping("/items")
    public List<Map<String, Object>> items() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (VectorItem item : vectorStore.all()) {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", item.id());
            m.put("metadata", item.metadata());
            m.put("category", item.category());
            m.put("embedding", item.embedding());
            out.add(m);
        }
        return out;
    }

    @GetMapping("/benchmark")
    public ResponseEntity<?> benchmark(
            @RequestParam("v") String v,
            @RequestParam(value = "k", defaultValue = "5") int k,
            @RequestParam(value = "metric", defaultValue = "cosine") String metric) {
        float[] query = parseVector(v);
        if (query.length != VectorStore.DIMS) {
            return ResponseEntity.ok(Map.of("error", "need " + VectorStore.DIMS + "D vector"));
        }
        BenchmarkResult result = vectorStore.benchmark(query, k, metric);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/hnsw-info")
    public HnswIndex.GraphInfo hnswInfo() {
        return vectorStore.hnswInfo();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of(
                "count", vectorStore.size(),
                "dims", VectorStore.DIMS,
                "algorithms", List.of("bruteforce", "kdtree", "hnsw"),
                "metrics", List.of("euclidean", "cosine", "manhattan"));
    }

    /** Parses a comma-separated float list ("0.1,0.2,..."), skipping unparseable tokens. */
    private float[] parseVector(String csv) {
        if (csv == null || csv.isBlank()) {
            return new float[0];
        }
        String[] parts = csv.split(",");
        List<Float> values = new ArrayList<>();
        for (String part : parts) {
            try {
                values.add(Float.parseFloat(part.trim()));
            } catch (NumberFormatException ignored) {
                // Silently skips tokens that don't parse as a float.
            }
        }
        float[] out = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            out[i] = values.get(i);
        }
        return out;
    }
}
