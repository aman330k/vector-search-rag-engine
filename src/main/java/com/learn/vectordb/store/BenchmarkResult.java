package com.learn.vectordb.store;

/**
 * Timings (in microseconds) from running the same query through all three algorithms.
 *
 * @param bruteforceUs brute-force latency
 * @param kdtreeUs     KD-Tree latency
 * @param hnswUs       HNSW latency
 * @param itemCount    number of items searched
 */
public record BenchmarkResult(long bruteforceUs, long kdtreeUs, long hnswUs, int itemCount) {
}
