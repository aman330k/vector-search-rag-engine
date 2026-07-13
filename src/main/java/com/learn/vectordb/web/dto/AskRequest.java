package com.learn.vectordb.web.dto;

/**
 * Body for {@code POST /doc/ask} and {@code POST /doc/search}.
 *
 * @param question the user's question
 * @param k        number of chunks to retrieve (defaults to 3 when null/0)
 */
public record AskRequest(String question, Integer k) {

    /** @return k, or 3 if not provided. */
    public int effectiveK() {
        return (k == null || k <= 0) ? 3 : k;
    }
}
