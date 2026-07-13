package com.learn.vectordb.model;

/**
 * A lightweight (distance, id) pair returned by the low-level indexes.
 *
 * <p>Implements {@link Comparable} so lists of neighbors sort by ascending distance (closest
 * first), which is exactly how k-NN results should be ordered.
 *
 * @param distance distance from the query vector (smaller is closer)
 * @param id       id of the stored item
 */
public record Neighbor(float distance, int id) implements Comparable<Neighbor> {

    @Override
    public int compareTo(Neighbor other) {
        int byDistance = Float.compare(this.distance, other.distance);
        return byDistance != 0 ? byDistance : Integer.compare(this.id, other.id);
    }
}
