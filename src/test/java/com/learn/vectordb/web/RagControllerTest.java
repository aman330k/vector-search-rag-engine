package com.learn.vectordb.web;

import com.learn.vectordb.embedding.OllamaClient;
import com.learn.vectordb.model.RagContext;
import com.learn.vectordb.model.RagResponse;
import com.learn.vectordb.rag.RagService;
import com.learn.vectordb.store.DocumentStore;
import com.learn.vectordb.store.VectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice test. Ollama and the RAG service are mocked, so this runs with no external
 * dependencies (no Ollama install needed).
 */
@WebMvcTest(RagController.class)
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RagService ragService;

    @MockBean
    private OllamaClient ollamaClient;

    @MockBean
    private DocumentStore documentStore;

    @MockBean
    private VectorStore vectorStore;

    @Test
    void askReturnsAnswerAndContexts() throws Exception {
        RagResponse response = new RagResponse(
                "Dynamic programming solves overlapping subproblems.",
                "llama3.2",
                List.of(new RagContext(1, "DP notes", "memoization...", 0.12f)),
                1);
        when(ragService.ask(anyString(), anyInt())).thenReturn(response);

        mockMvc.perform(post("/doc/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"what is dp?\",\"k\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(
                        "Dynamic programming solves overlapping subproblems."))
                .andExpect(jsonPath("$.model").value("llama3.2"))
                .andExpect(jsonPath("$.contexts[0].title").value("DP notes"));
    }

    @Test
    void askWithBlankQuestionReturnsError() throws Exception {
        mockMvc.perform(post("/doc/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\",\"k\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("need question"));
    }

    @Test
    void askReturnsErrorWhenOllamaUnavailable() throws Exception {
        when(ragService.ask(anyString(), anyInt()))
                .thenThrow(new RagService.OllamaUnavailableException("Ollama unavailable"));

        mockMvc.perform(post("/doc/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"hi\",\"k\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("Ollama unavailable"));
    }

    @Test
    void statusReportsModelsAndCounts() throws Exception {
        when(ollamaClient.isAvailable()).thenReturn(true);
        when(ollamaClient.getEmbedModel()).thenReturn("nomic-embed-text");
        when(ollamaClient.getGenModel()).thenReturn("llama3.2");
        when(documentStore.size()).thenReturn(4);
        when(documentStore.getDims()).thenReturn(768);
        when(vectorStore.size()).thenReturn(20);

        mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ollamaAvailable").value(true))
                .andExpect(jsonPath("$.embedModel").value("nomic-embed-text"))
                .andExpect(jsonPath("$.genModel").value("llama3.2"))
                .andExpect(jsonPath("$.docCount").value(4))
                .andExpect(jsonPath("$.demoCount").value(20));
    }
}
