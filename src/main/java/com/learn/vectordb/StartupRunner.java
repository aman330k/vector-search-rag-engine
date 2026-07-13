package com.learn.vectordb;

import com.learn.vectordb.embedding.OllamaClient;
import com.learn.vectordb.store.DemoDataLoader;
import com.learn.vectordb.store.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Loads the demo vectors at startup and prints a status banner.
 */
@Component
public class StartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);

    private final VectorStore vectorStore;
    private final DemoDataLoader demoDataLoader;
    private final OllamaClient ollama;

    @Value("${server.port}")
    private String serverPort;

    public StartupRunner(VectorStore vectorStore, DemoDataLoader demoDataLoader,
                         OllamaClient ollama) {
        this.vectorStore = vectorStore;
        this.demoDataLoader = demoDataLoader;
        this.ollama = ollama;
    }

    @Override
    public void run(ApplicationArguments args) {
        demoDataLoader.load(vectorStore);
        boolean ollamaUp = ollama.isAvailable();

        log.info("=== VectorDB Engine (Java) ===");
        log.info("http://localhost:{}", serverPort);
        log.info("{} demo vectors | {} dims | HNSW+KD-Tree+BruteForce",
                vectorStore.size(), VectorStore.DIMS);
        log.info("Ollama: {}", ollamaUp ? "ONLINE" : "OFFLINE (install from ollama.com)");
        if (ollamaUp) {
            log.info("  embed model: {}  gen model: {}",
                    ollama.getEmbedModel(), ollama.getGenModel());
        }
    }
}
