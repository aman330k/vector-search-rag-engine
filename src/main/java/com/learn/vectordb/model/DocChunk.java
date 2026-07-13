package com.learn.vectordb.model;

/**
 * One chunk of an ingested document, together with its real embedding from Ollama.
 *
 * <p>Long documents are split into several overlapping chunks, and each chunk becomes one of
 * these records.
 *
 * @param id        unique identifier assigned by the document store
 * @param title     display title (may include a "[2/5]" chunk suffix)
 * @param text      the raw chunk text used as RAG context
 * @param embedding the vector produced by the embedding model (e.g. 768-D)
 */
public record DocChunk(int id, String title, String text, float[] embedding) {
}
