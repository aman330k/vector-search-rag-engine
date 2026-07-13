package com.learn.vectordb.index;

import com.learn.vectordb.distance.DistanceMetric;
import com.learn.vectordb.model.Neighbor;
import com.learn.vectordb.model.VectorItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The simplest possible index: compare the query against every stored vector.
 *
 * <p>Complexity is O(N·d) per query (N items, d dimensions). It is always 100% accurate, which
 * makes it the perfect "ground truth" to measure the approximate HNSW index against (see the
 * evaluation harness).
 */
public class BruteForceIndex implements VectorIndex {

    private final List<VectorItem> items = new ArrayList<>();

    @Override
    public void insert(VectorItem item, DistanceMetric metric) {
        items.add(item);
    }

    @Override
    public List<Neighbor> knn(float[] query, int k, DistanceMetric metric) {
        List<Neighbor> results = new ArrayList<>(items.size());
        for (VectorItem item : items) {
            results.add(new Neighbor(metric.distance(query, item.embedding()), item.id()));
        }
        Collections.sort(results);
        if (results.size() > k) {
            return new ArrayList<>(results.subList(0, k));
        }
        return results;
    }

    @Override
    public void remove(int id) {
        items.removeIf(item -> item.id() == id);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public String name() {
        return "bruteforce";
    }
}
