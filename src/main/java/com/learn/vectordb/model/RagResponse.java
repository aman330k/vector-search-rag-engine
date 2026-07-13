package com.learn.vectordb.model;

import java.util.List;

/**
 * The full payload of a {@code /doc/ask} (RAG) response.
 *
 * @param answer   the LLM's generated answer
 * @param model    which generation model produced it
 * @param contexts the retrieved chunks that were injected into the prompt
 * @param docCount how many chunks are currently stored
 */
public record RagResponse(String answer, String model, List<RagContext> contexts, int docCount) {
}
