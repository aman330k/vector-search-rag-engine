package com.learn.vectordb.store;

import com.learn.vectordb.distance.DistanceMetric;
import com.learn.vectordb.index.BruteForceIndex;
import com.learn.vectordb.index.HnswIndex;
import com.learn.vectordb.model.DocChunk;
import com.learn.vectordb.model.Neighbor;
import org.springframework.stereotype.Component;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores real document-chunk embeddings (e.g. 768-D from Ollama) and serves semantic search.
 *
 * <p>Uses HNSW for retrieval, with a brute-force fallback while the set is tiny (< 10 chunks)
 * so results stay exact before the graph is worth building. Cosine distance is always used for
 * text embeddings.
 */
@Component
public class DocumentStore {

    private static final int BRUTE_FORCE_THRESHOLD = 10;
    private static final float DEFAULT_MAX_DISTANCE = 0.7f;

    private final Map<Integer, DocChunk> store = new LinkedHashMap<>();
    private HnswIndex hnsw = new HnswIndex(16, 200);
    private BruteForceIndex bruteForce = new BruteForceIndex();
    private int nextId = 1;
    private int dims = 0;

    /**
     * Inserts one chunk with its precomputed embedding.
     *
     * @param title chunk title
     * @param text  chunk text
     * @param embedding the chunk's vector
     * @return the assigned chunk id
     */
    public synchronized int insert(String title, String text, float[] embedding) {
        if (dims == 0) {
            dims = embedding.length;
        }
        DocChunk chunk = new DocChunk(nextId++, title, text, embedding);
        store.put(chunk.id(), chunk);
        var vectorItem = new com.learn.vectordb.model.VectorItem(chunk.id(), title, "doc", embedding);
        hnsw.insert(vectorItem, DistanceMetric.COSINE);
        bruteForce.insert(vectorItem, DistanceMetric.COSINE);
        return chunk.id();
    }

    /**
     * Semantic search: returns the top-k most similar chunks within a distance threshold.
     *
     * @param query   the query embedding
     * @param k       number of chunks to return
     * @param maxDist maximum cosine distance to accept (filters out weak matches)
     * @return matching chunks paired with their distance, closest first
     */
    public synchronized List<Map.Entry<Float, DocChunk>> search(float[] query, int k, float maxDist) {
        if (store.isEmpty()) {
            return new ArrayList<>();
        }
        List<Neighbor> raw = (store.size() < BRUTE_FORCE_THRESHOLD)
                ? bruteForce.knn(query, k, DistanceMetric.COSINE)
                : hnsw.knn(query, k, 50, DistanceMetric.COSINE);

        List<Map.Entry<Float, DocChunk>> out = new ArrayList<>();
        for (Neighbor n : raw) {
            DocChunk chunk = store.get(n.id());
            if (chunk != null && n.distance() <= maxDist) {
                out.add(new SimpleEntry<>(n.distance(), chunk));
            }
        }
        return out;
    }

    /** Convenience overload using the default max-distance threshold. */
    public synchronized List<Map.Entry<Float, DocChunk>> search(float[] query, int k) {
        return search(query, k, DEFAULT_MAX_DISTANCE);
    }

    /**
     * Removes a chunk by id.
     *
     * @param id chunk id
     * @return true if it existed
     */
    public synchronized boolean remove(int id) {
        if (!store.containsKey(id)) {
            return false;
        }
        store.remove(id);
        hnsw.remove(id);
        bruteForce.remove(id);
        return true;
    }

    /** @return all stored chunks in insertion order. */
    public synchronized List<DocChunk> all() {
        return new ArrayList<>(store.values());
    }

    /** @return number of stored chunks. */
    public synchronized int size() {
        return store.size();
    }

    /** @return embedding dimensionality (0 until the first insert). */
    public synchronized int getDims() {
        return dims;
    }

    /**
     * Replaces the entire store with the given chunks and rebuilds the indexes. Used by
     * persistence when loading a snapshot from disk.
     *
     * @param chunks the chunks to load
     */
    public synchronized void replaceAll(List<DocChunk> chunks) {
        store.clear();
        hnsw = new HnswIndex(16, 200);
        bruteForce = new BruteForceIndex();
        nextId = 1;
        dims = 0;
        for (DocChunk chunk : chunks) {
            if (dims == 0) {
                dims = chunk.embedding().length;
            }
            store.put(chunk.id(), chunk);
            var vectorItem = new com.learn.vectordb.model.VectorItem(
                    chunk.id(), chunk.title(), "doc", chunk.embedding());
            hnsw.insert(vectorItem, DistanceMetric.COSINE);
            bruteForce.insert(vectorItem, DistanceMetric.COSINE);
            nextId = Math.max(nextId, chunk.id() + 1);
        }
    }
}
