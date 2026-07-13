package com.learn.vectordb.model;

import java.util.List;

/**
 * The full payload of a {@code /search} response.
 *
 * @param results   ranked hits, closest first
 * @param latencyUs how long the search took, in microseconds (for the benchmark UI)
 * @param algo      which algorithm ran ("bruteforce", "kdtree", or "hnsw")
 * @param metric    which distance metric was used
 */
public record SearchResponse(List<SearchHit> results, long latencyUs, String algo, String metric) {
}
