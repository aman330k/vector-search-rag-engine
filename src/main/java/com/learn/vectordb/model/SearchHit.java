package com.learn.vectordb.model;

/**
 * One row of a search result returned to the client.
 *
 * @param id        item id
 * @param metadata  human-readable description
 * @param category  category label
 * @param distance  distance from the query (smaller is more similar)
 * @param embedding the stored vector (used by the UI to draw the scatter plot)
 */
public record SearchHit(int id, String metadata, String category, float distance, float[] embedding) {
}
