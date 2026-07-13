package com.learn.vectordb.model;

/**
 * One retrieved chunk that was fed to the LLM as context, shown as a "context chip" in the UI.
 *
 * @param id       chunk id
 * @param title    chunk title
 * @param text     chunk text (the actual context the model saw)
 * @param distance similarity distance to the question
 */
public record RagContext(int id, String title, String text, float distance) {
}
