package com.learn.vectordb.model;

/**
 * A single stored vector: a numeric embedding plus human-readable labels.
 *
 * <p>Used both for the 16-D demo vectors and, internally, as the payload the indexes store.
 *
 * @param id       unique identifier assigned by the store
 * @param metadata human-readable description (e.g. "Binary Search Tree...")
 * @param category coarse label used for coloring and metadata filtering (e.g. "cs", "math")
 * @param embedding the vector itself
 */
public record VectorItem(int id, String metadata, String category, float[] embedding) {
}
