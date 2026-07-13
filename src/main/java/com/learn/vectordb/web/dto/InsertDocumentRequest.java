package com.learn.vectordb.web.dto;

/**
 * Body for {@code POST /doc/insert}: a document to chunk, embed, and store.
 *
 * @param title document title
 * @param text  document body
 */
public record InsertDocumentRequest(String title, String text) {
}
