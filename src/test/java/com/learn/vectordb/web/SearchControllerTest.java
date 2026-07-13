package com.learn.vectordb.web;

import com.learn.vectordb.model.SearchHit;
import com.learn.vectordb.model.SearchResponse;
import com.learn.vectordb.store.VectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VectorStore vectorStore;

    private String vec(int dims) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dims; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("0.1");
        }
        return sb.toString();
    }

    @Test
    void searchReturnsRankedResults() throws Exception {
        SearchResponse response = new SearchResponse(
                List.of(new SearchHit(3, "Hash Table", "cs", 0.05f, new float[]{0.1f})),
                42L, "hnsw", "cosine");
        when(vectorStore.search(any(), anyInt(), anyString(), anyString(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/search")
                        .param("v", vec(16))
                        .param("k", "5")
                        .param("metric", "cosine")
                        .param("algo", "hnsw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].metadata").value("Hash Table"))
                .andExpect(jsonPath("$.algo").value("hnsw"))
                .andExpect(jsonPath("$.latencyUs").value(42));
    }

    @Test
    void searchWithWrongDimensionsReturnsError() throws Exception {
        mockMvc.perform(get("/search").param("v", vec(4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("need 16D vector"));
    }

    @Test
    void deleteReturnsOkFlag() throws Exception {
        when(vectorStore.remove(eq(7))).thenReturn(true);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/delete/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void statsListsAlgorithmsAndMetrics() throws Exception {
        when(vectorStore.size()).thenReturn(20);
        mockMvc.perform(get("/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(20))
                .andExpect(jsonPath("$.dims").value(16))
                .andExpect(jsonPath("$.algorithms[0]").value("bruteforce"));
    }
}
