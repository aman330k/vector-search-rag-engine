package com.learn.vectordb.index;

import com.learn.vectordb.distance.DistanceMetric;
import com.learn.vectordb.model.Neighbor;
import com.learn.vectordb.model.VectorItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the indexes against a brute-force oracle. Brute force is exact by definition, so it
 * is the ground truth every other index is measured against.
 */
class IndexCorrectnessTest {

    private static final int DIMS = 8;
    private static final int N = 200;

    private List<VectorItem> randomItems(long seed) {
        Random rng = new Random(seed);
        List<VectorItem> items = new ArrayList<>();
        for (int i = 1; i <= N; i++) {
            float[] v = new float[DIMS];
            for (int d = 0; d < DIMS; d++) {
                v[d] = rng.nextFloat();
            }
            items.add(new VectorItem(i, "item-" + i, "cat", v));
        }
        return items;
    }

    private Set<Integer> ids(List<Neighbor> neighbors) {
        Set<Integer> s = new HashSet<>();
        for (Neighbor n : neighbors) {
            s.add(n.id());
        }
        return s;
    }

    @Test
    void kdTreeMatchesBruteForceExactly() {
        List<VectorItem> items = randomItems(1L);
        BruteForceIndex bf = new BruteForceIndex();
        KdTreeIndex kd = new KdTreeIndex(DIMS);
        for (VectorItem item : items) {
            bf.insert(item, DistanceMetric.EUCLIDEAN);
            kd.insert(item, DistanceMetric.EUCLIDEAN);
        }

        Random rng = new Random(99L);
        int k = 10;
        for (int q = 0; q < 25; q++) {
            float[] query = new float[DIMS];
            for (int d = 0; d < DIMS; d++) {
                query[d] = rng.nextFloat();
            }
            List<Neighbor> expected = bf.knn(query, k, DistanceMetric.EUCLIDEAN);
            List<Neighbor> actual = kd.knn(query, k, DistanceMetric.EUCLIDEAN);
            assertEquals(ids(expected), ids(actual),
                    "KD-Tree must return the same neighbor set as brute force");
        }
    }

    @Test
    void hnswFindsExactPointWhenQueryingAStoredVector() {
        List<VectorItem> items = randomItems(2L);
        HnswIndex hnsw = new HnswIndex(16, 200);
        for (VectorItem item : items) {
            hnsw.insert(item, DistanceMetric.EUCLIDEAN);
        }
        // Querying with a stored vector should return that exact point (distance ~0) as top-1.
        for (VectorItem item : items) {
            List<Neighbor> result = hnsw.knn(item.embedding(), 1, DistanceMetric.EUCLIDEAN);
            assertEquals(1, result.size());
            assertEquals(0f, result.get(0).distance(), 1e-4f);
        }
    }

    @Test
    void hnswRecallAgainstBruteForceIsHigh() {
        List<VectorItem> items = randomItems(3L);
        BruteForceIndex bf = new BruteForceIndex();
        HnswIndex hnsw = new HnswIndex(16, 200);
        for (VectorItem item : items) {
            bf.insert(item, DistanceMetric.EUCLIDEAN);
            hnsw.insert(item, DistanceMetric.EUCLIDEAN);
        }

        Random rng = new Random(7L);
        int k = 10;
        double recallSum = 0;
        int queries = 30;
        for (int q = 0; q < queries; q++) {
            float[] query = new float[DIMS];
            for (int d = 0; d < DIMS; d++) {
                query[d] = rng.nextFloat();
            }
            Set<Integer> exact = ids(bf.knn(query, k, DistanceMetric.EUCLIDEAN));
            Set<Integer> approx = ids(hnsw.knn(query, k, 50, DistanceMetric.EUCLIDEAN));
            approx.retainAll(exact);
            recallSum += (double) approx.size() / k;
        }
        double recall = recallSum / queries;
        assertTrue(recall >= 0.8, "HNSW recall@" + k + " should be high, was " + recall);
    }

    @Test
    void bruteForceRemoveWorks() {
        BruteForceIndex bf = new BruteForceIndex();
        List<VectorItem> items = randomItems(4L);
        for (VectorItem item : items) {
            bf.insert(item, DistanceMetric.EUCLIDEAN);
        }
        assertEquals(N, bf.size());
        bf.remove(1);
        assertEquals(N - 1, bf.size());
        Set<Integer> remaining = ids(bf.knn(items.get(0).embedding(), N, DistanceMetric.EUCLIDEAN));
        assertTrue(!remaining.contains(1));
    }
}
