package com.learn.vectordb.index;

import com.learn.vectordb.distance.DistanceMetric;
import com.learn.vectordb.model.Neighbor;
import com.learn.vectordb.model.VectorItem;

import java.util.List;

/**
 * Common contract for all nearest-neighbor indexes (BruteForce, KD-Tree, HNSW).
 *
 * <p>An "index" is just a data structure that stores vectors and can answer the question:
 * "which stored vectors are closest to this query vector?" Different indexes trade accuracy
 * for speed in different ways — see the {@code docs/} folder.
 */
public interface VectorIndex {

    /**
     * Adds one item to the index.
     *
     * @param item   the item to store
     * @param metric distance metric the index should use when building its structure
     */
    void insert(VectorItem item, DistanceMetric metric);

    /**
     * Finds the {@code k} nearest neighbors of the query vector.
     *
     * @param query  the query vector
     * @param k      how many neighbors to return
     * @param metric distance metric to rank by
     * @return up to {@code k} neighbors, sorted closest-first
     */
    List<Neighbor> knn(float[] query, int k, DistanceMetric metric);

    /**
     * Removes an item by id (no-op if absent).
     *
     * @param id the item id to remove
     */
    void remove(int id);

    /** @return number of items currently in the index. */
    int size();

    /** @return a short human-friendly name ("bruteforce", "kdtree", "hnsw"). */
    String name();
}
