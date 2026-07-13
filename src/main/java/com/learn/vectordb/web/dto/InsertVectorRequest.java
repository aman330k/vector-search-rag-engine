package com.learn.vectordb.web.dto;

/**
 * Body for {@code POST /insert}: a demo vector to add.
 *
 * @param metadata  human-readable description
 * @param category  category label
 * @param embedding the 16-D vector
 */
public record InsertVectorRequest(String metadata, String category, float[] embedding) {
}
