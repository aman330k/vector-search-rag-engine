package com.learn.vectordb.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.vectordb.config.VectorDbProperties;
import com.learn.vectordb.model.DocChunk;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Saves and loads the {@link DocumentStore} to/from a JSON snapshot on disk.
 *
 * <p>Persistence is a basic durability concern that shows up constantly in real systems (and
 * interviews). We use the simplest approach that teaches the idea clearly: serialize the whole
 * chunk list to JSON, load it back on startup.
 */
@Component
public class SnapshotRepository {

    private static final Logger log = LoggerFactory.getLogger(SnapshotRepository.class);

    private final VectorDbProperties.Persistence config;
    private final DocumentStore documentStore;
    private final ObjectMapper mapper = new ObjectMapper();

    public SnapshotRepository(VectorDbProperties properties, DocumentStore documentStore) {
        this.config = properties.getPersistence();
        this.documentStore = documentStore;
    }

    @PostConstruct
    void loadOnStartup() {
        if (config.isEnabled()) {
            int loaded = load();
            if (loaded > 0) {
                log.info("Loaded {} document chunks from snapshot {}", loaded,
                        config.getSnapshotFile());
            }
        }
    }

    @PreDestroy
    void saveOnShutdown() {
        if (config.isEnabled()) {
            save();
        }
    }

    /**
     * Writes the current document store to the snapshot file.
     *
     * @return the number of chunks written
     */
    public synchronized int save() {
        List<DocChunk> chunks = documentStore.all();
        try {
            Path path = Path.of(config.getSnapshotFile());
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), chunks);
            log.info("Saved {} document chunks to {}", chunks.size(), path);
            return chunks.size();
        } catch (IOException e) {
            log.warn("Failed to save snapshot: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Loads the document store from the snapshot file (replacing current contents).
     *
     * @return the number of chunks loaded (0 if the file is missing or unreadable)
     */
    public synchronized int load() {
        try {
            Path path = Path.of(config.getSnapshotFile());
            if (!Files.exists(path)) {
                return 0;
            }
            DocChunk[] chunks = mapper.readValue(path.toFile(), DocChunk[].class);
            documentStore.replaceAll(List.of(chunks));
            return chunks.length;
        } catch (IOException e) {
            log.warn("Failed to load snapshot: {}", e.getMessage());
            return 0;
        }
    }
}
