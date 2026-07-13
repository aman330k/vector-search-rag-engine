package com.learn.vectordb.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learn.vectordb.config.VectorDbProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin HTTP client around a locally running <a href="https://ollama.com">Ollama</a> server.
 *
 * <p>Wraps two endpoints:
 * <ul>
 *   <li>{@code POST /api/embeddings} — turn text into a vector (model {@code nomic-embed-text})</li>
 *   <li>{@code POST /api/generate} — ask the LLM to write an answer (model {@code llama3.2})</li>
 * </ul>
 * Uses the JDK's built-in {@link HttpClient}, so no extra dependency is needed.
 */
@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final VectorDbProperties.Ollama config;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient;

    public OllamaClient(VectorDbProperties properties) {
        this.config = properties.getOllama();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getConnectTimeoutSeconds()))
                .build();
    }

    /** @return the configured embedding model name (e.g. "nomic-embed-text"). */
    public String getEmbedModel() {
        return config.getEmbedModel();
    }

    /** @return the configured generation model name (e.g. "llama3.2"). */
    public String getGenModel() {
        return config.getGenModel();
    }

    /**
     * Quick health check: is Ollama reachable?
     *
     * @return true if {@code /api/tags} responds 200 within the connect timeout
     */
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(config.getConnectTimeoutSeconds()))
                    .GET()
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Embeds a piece of text into a vector.
     *
     * @param text the text to embed
     * @return the embedding vector, or an empty array if Ollama is unavailable / errored
     */
    public float[] embed(String text) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", config.getEmbedModel());
            body.put("prompt", text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/api/embeddings"))
                    .timeout(Duration.ofSeconds(config.getEmbedTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Ollama embeddings returned status {}", response.statusCode());
                return new float[0];
            }
            JsonNode node = mapper.readTree(response.body()).path("embedding");
            if (!node.isArray()) {
                return new float[0];
            }
            float[] out = new float[node.size()];
            for (int i = 0; i < node.size(); i++) {
                out[i] = (float) node.get(i).asDouble();
            }
            return out;
        } catch (Exception e) {
            log.warn("Ollama embed failed: {}", e.getMessage());
            return new float[0];
        }
    }

    /**
     * Asks the generation model to produce an answer for the given prompt (non-streaming).
     *
     * @param prompt the full prompt (question + retrieved context)
     * @return the model's answer, or an error string if Ollama is unavailable
     */
    public String generate(String prompt) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", config.getGenModel());
            body.put("prompt", prompt);
            body.put("stream", false);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/api/generate"))
                    .timeout(Duration.ofSeconds(config.getGenerateTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return "ERROR: Ollama unavailable. Run: ollama serve";
            }
            return mapper.readTree(response.body()).path("response").asText("");
        } catch (Exception e) {
            log.warn("Ollama generate failed: {}", e.getMessage());
            return "ERROR: Ollama unavailable. Run: ollama serve";
        }
    }
}
