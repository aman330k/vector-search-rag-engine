package com.learn.vectordb.eval;

import java.util.List;

/**
 * Result of a recall@k evaluation run.
 *
 * @param k              the k that was measured
 * @param metric         distance metric used
 * @param queryCount     how many queries were evaluated
 * @param perAlgorithm   one row per approximate/exact algorithm being scored
 */
public record RecallReport(int k, String metric, int queryCount, List<AlgorithmScore> perAlgorithm) {

    /**
     * Score for a single algorithm.
     *
     * @param algorithm     algorithm name
     * @param recallAtK     average fraction of the exact top-k that this algorithm found (0..1)
     * @param avgLatencyUs  average query latency in microseconds
     */
    public record AlgorithmScore(String algorithm, double recallAtK, double avgLatencyUs) {
    }
}
