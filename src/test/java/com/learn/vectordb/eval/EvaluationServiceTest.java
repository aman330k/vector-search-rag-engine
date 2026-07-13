package com.learn.vectordb.eval;

import com.learn.vectordb.store.DemoDataLoader;
import com.learn.vectordb.store.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationServiceTest {

    private EvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        VectorStore store = new VectorStore();
        new DemoDataLoader().load(store);
        evaluationService = new EvaluationService(store);
    }

    @Test
    void kdTreeIsExactSoRecallIsOne() {
        RecallReport report = evaluationService.evaluate(5, "cosine");
        RecallReport.AlgorithmScore kd = report.perAlgorithm().stream()
                .filter(a -> a.algorithm().equals("kdtree"))
                .findFirst().orElseThrow();
        assertEquals(1.0, kd.recallAtK(), 1e-9,
                "KD-Tree is exact, so recall@k against brute force must be 1.0");
    }

    @Test
    void hnswRecallIsHighOnDemoData() {
        RecallReport report = evaluationService.evaluate(5, "cosine");
        RecallReport.AlgorithmScore hnsw = report.perAlgorithm().stream()
                .filter(a -> a.algorithm().equals("hnsw"))
                .findFirst().orElseThrow();
        assertTrue(hnsw.recallAtK() >= 0.8,
                "HNSW recall should be high, was " + hnsw.recallAtK());
    }

    @Test
    void reportReflectsQueryCount() {
        RecallReport report = evaluationService.evaluate(3, "cosine");
        assertEquals(20, report.queryCount());
        assertEquals(3, report.k());
    }
}
