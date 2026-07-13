package com.learn.vectordb.web;

import com.learn.vectordb.eval.EvaluationService;
import com.learn.vectordb.eval.RecallReport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint for the RAG evaluation harness.
 *
 * <p>{@code GET /eval/recall?k=5&metric=cosine} runs recall@k for KD-Tree and HNSW against the
 * exact brute-force ground truth over the current demo vectors.
 */
@RestController
public class EvalController {

    private final EvaluationService evaluationService;

    public EvalController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/eval/recall")
    public RecallReport recall(
            @RequestParam(value = "k", defaultValue = "5") int k,
            @RequestParam(value = "metric", defaultValue = "cosine") String metric) {
        return evaluationService.evaluate(k, metric);
    }
}
