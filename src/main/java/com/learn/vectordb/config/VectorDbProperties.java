package com.learn.vectordb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Strongly-typed view of the {@code vectordb.*} section in {@code application.yml}.
 *
 * <p>Using a properties class (instead of scattering {@code @Value} annotations) keeps
 * configuration in one place and is the idiomatic Spring Boot approach.
 */
@ConfigurationProperties(prefix = "vectordb")
public class VectorDbProperties {

    @NestedConfigurationProperty
    private Ollama ollama = new Ollama();

    @NestedConfigurationProperty
    private Persistence persistence = new Persistence();

    @NestedConfigurationProperty
    private Chunking chunking = new Chunking();

    public Ollama getOllama() {
        return ollama;
    }

    public void setOllama(Ollama ollama) {
        this.ollama = ollama;
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
    }

    public Chunking getChunking() {
        return chunking;
    }

    public void setChunking(Chunking chunking) {
        this.chunking = chunking;
    }

    /** Connection details for the local Ollama server. */
    public static class Ollama {
        private String baseUrl = "http://127.0.0.1:11434";
        private String embedModel = "nomic-embed-text";
        private String genModel = "llama3.2";
        private int connectTimeoutSeconds = 3;
        private int embedTimeoutSeconds = 30;
        private int generateTimeoutSeconds = 180;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getEmbedModel() {
            return embedModel;
        }

        public void setEmbedModel(String embedModel) {
            this.embedModel = embedModel;
        }

        public String getGenModel() {
            return genModel;
        }

        public void setGenModel(String genModel) {
            this.genModel = genModel;
        }

        public int getConnectTimeoutSeconds() {
            return connectTimeoutSeconds;
        }

        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
        }

        public int getEmbedTimeoutSeconds() {
            return embedTimeoutSeconds;
        }

        public void setEmbedTimeoutSeconds(int embedTimeoutSeconds) {
            this.embedTimeoutSeconds = embedTimeoutSeconds;
        }

        public int getGenerateTimeoutSeconds() {
            return generateTimeoutSeconds;
        }

        public void setGenerateTimeoutSeconds(int generateTimeoutSeconds) {
            this.generateTimeoutSeconds = generateTimeoutSeconds;
        }
    }

    /** Where and whether the document store is persisted to disk. */
    public static class Persistence {
        private boolean enabled = true;
        private String snapshotFile = "./data/documents-snapshot.json";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSnapshotFile() {
            return snapshotFile;
        }

        public void setSnapshotFile(String snapshotFile) {
            this.snapshotFile = snapshotFile;
        }
    }

    /** Default document chunking parameters. */
    public static class Chunking {
        private String strategy = "word";
        private int chunkWords = 250;
        private int overlapWords = 30;

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public int getChunkWords() {
            return chunkWords;
        }

        public void setChunkWords(int chunkWords) {
            this.chunkWords = chunkWords;
        }

        public int getOverlapWords() {
            return overlapWords;
        }

        public void setOverlapWords(int overlapWords) {
            this.overlapWords = overlapWords;
        }
    }
}
