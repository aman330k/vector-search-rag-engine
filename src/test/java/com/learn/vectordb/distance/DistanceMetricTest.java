package com.learn.vectordb.distance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistanceMetricTest {

    private static final float EPS = 1e-5f;

    @Test
    void euclideanOfKnownVectors() {
        float[] a = {0f, 0f};
        float[] b = {3f, 4f};
        assertEquals(5f, DistanceMetric.EUCLIDEAN.distance(a, b), EPS);
    }

    @Test
    void manhattanOfKnownVectors() {
        float[] a = {1f, 2f, 3f};
        float[] b = {4f, 0f, 3f};
        // |1-4| + |2-0| + |3-3| = 3 + 2 + 0 = 5
        assertEquals(5f, DistanceMetric.MANHATTAN.distance(a, b), EPS);
    }

    @Test
    void cosineOfIdenticalDirectionIsZero() {
        float[] a = {1f, 2f, 3f};
        float[] b = {2f, 4f, 6f}; // same direction, different magnitude
        assertEquals(0f, DistanceMetric.COSINE.distance(a, b), EPS);
    }

    @Test
    void cosineOfOrthogonalVectorsIsOne() {
        float[] a = {1f, 0f};
        float[] b = {0f, 1f};
        assertEquals(1f, DistanceMetric.COSINE.distance(a, b), EPS);
    }

    @Test
    void cosineOfZeroVectorIsSafe() {
        float[] a = {0f, 0f};
        float[] b = {1f, 1f};
        assertEquals(1f, DistanceMetric.COSINE.distance(a, b), EPS);
    }

    @Test
    void fromNameIsCaseInsensitiveAndDefaultsToEuclidean() {
        assertEquals(DistanceMetric.COSINE, DistanceMetric.fromName("Cosine"));
        assertEquals(DistanceMetric.MANHATTAN, DistanceMetric.fromName("MANHATTAN"));
        assertEquals(DistanceMetric.EUCLIDEAN, DistanceMetric.fromName("unknown"));
        assertEquals(DistanceMetric.EUCLIDEAN, DistanceMetric.fromName(null));
    }

    @Test
    void closerVectorHasSmallerDistance() {
        float[] query = {1f, 1f};
        float[] near = {1.1f, 1.1f};
        float[] far = {5f, 5f};
        assertTrue(DistanceMetric.EUCLIDEAN.distance(query, near)
                < DistanceMetric.EUCLIDEAN.distance(query, far));
    }
}
