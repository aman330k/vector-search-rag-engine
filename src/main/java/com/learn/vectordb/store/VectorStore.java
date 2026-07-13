package com.learn.vectordb.store;

import com.learn.vectordb.distance.DistanceMetric;
import com.learn.vectordb.index.BruteForceIndex;
import com.learn.vectordb.index.HnswIndex;
import com.learn.vectordb.index.KdTreeIndex;
import com.learn.vectordb.model.Neighbor;
import com.learn.vectordb.model.SearchHit;
import com.learn.vectordb.model.SearchResponse;
import com.learn.vectordb.model.VectorItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The demo vector database over small, hand-built 16-dimensional vectors.
 *
 * <p>Keeps the same item in all three indexes at once so you can compare BruteForce, KD-Tree
 * and HNSW on identical data. Adds an optional category filter (a mid/senior-relevant feature
 * real vector DBs all provide).
 */
@Component
public class VectorStore {

    /** Dimensionality of the demo vectors. */
    public static final int DIMS = 16;

    private final Map<Integer, VectorItem> items = new LinkedHashMap<>();
    private final BruteForceIndex bruteForce = new BruteForceIndex();
    private final KdTreeIndex kdTree = new KdTreeIndex(DIMS);
    private final HnswIndex hnsw = new HnswIndex(16, 200);
    private int nextId = 1;

    /**
     * Inserts a new demo vector into all three indexes.
     *
     * @param metadata  human-readable description
     * @param category  category label
     * @param embedding 16-D vector
     * @param metric    metric used to build the HNSW graph
     * @return the id assigned to the new item
     */
    public synchronized int insert(String metadata, String category, float[] embedding,
                                   DistanceMetric metric) {
        VectorItem item = new VectorItem(nextId++, metadata, category, embedding);
        items.put(item.id(), item);
        bruteForce.insert(item, metric);
        kdTree.insert(item, metric);
        hnsw.insert(item, metric);
        return item.id();
    }

    /**
     * Removes an item by id. KD-Tree is rebuilt from scratch (it has no cheap delete).
     *
     * @param id item id
     * @return true if the item existed and was removed
     */
    public synchronized boolean remove(int id) {
        if (!items.containsKey(id)) {
            return false;
        }
        items.remove(id);
        bruteForce.remove(id);
        hnsw.remove(id);
        kdTree.rebuild(new ArrayList<>(items.values()));
        return true;
    }

    /**
     * Runs a k-NN search using the requested algorithm and metric, optionally restricted to a
     * single category.
     *
     * <p>When a {@code categoryFilter} is supplied we use exact "pre-filtering": we search only
     * the matching subset with brute force. This is always correct and easy to reason about.
     * Production systems (Pinecone/Weaviate) use more advanced filtered-HNSW; see the docs.
     *
     * @param query          16-D query vector
     * @param k              number of results
     * @param metricName     "cosine" | "euclidean" | "manhattan"
     * @param algo           "bruteforce" | "kdtree" | "hnsw"
     * @param categoryFilter optional category to restrict to (null/blank = no filter)
     * @return ranked results with timing
     */
    public synchronized SearchResponse search(float[] query, int k, String metricName,
                                               String algo, String categoryFilter) {
        DistanceMetric metric = DistanceMetric.fromName(metricName);
        String algorithm = (algo == null || algo.isBlank()) ? "hnsw" : algo.toLowerCase(Locale.ROOT);
        boolean filtered = categoryFilter != null && !categoryFilter.isBlank();

        long start = System.nanoTime();
        List<Neighbor> raw;
        if (filtered) {
            raw = filteredKnn(query, k, metric, categoryFilter);
        } else if ("bruteforce".equals(algorithm)) {
            raw = bruteForce.knn(query, k, metric);
        } else if ("kdtree".equals(algorithm)) {
            raw = kdTree.knn(query, k, metric);
        } else {
            raw = hnsw.knn(query, k, 50, metric);
        }
        long latencyUs = (System.nanoTime() - start) / 1000;

        List<SearchHit> hits = new ArrayList<>();
        for (Neighbor n : raw) {
            VectorItem item = items.get(n.id());
            if (item != null) {
                hits.add(new SearchHit(item.id(), item.metadata(), item.category(),
                        n.distance(), item.embedding()));
            }
        }
        return new SearchResponse(hits, latencyUs, filtered ? algorithm + "+filter" : algorithm,
                metric.apiName());
    }

    private List<Neighbor> filteredKnn(float[] query, int k, DistanceMetric metric,
                                       String category) {
        BruteForceIndex subset = new BruteForceIndex();
        for (VectorItem item : items.values()) {
            if (category.equalsIgnoreCase(item.category())) {
                subset.insert(item, metric);
            }
        }
        return subset.knn(query, k, metric);
    }

    /**
     * Times the same query on all three algorithms (no filtering).
     *
     * @param query      query vector
     * @param k          number of results
     * @param metricName metric name
     * @return per-algorithm timings
     */
    public synchronized BenchmarkResult benchmark(float[] query, int k, String metricName) {
        DistanceMetric metric = DistanceMetric.fromName(metricName);
        long bf = time(() -> bruteForce.knn(query, k, metric));
        long kd = time(() -> kdTree.knn(query, k, metric));
        long hn = time(() -> hnsw.knn(query, k, 50, metric));
        return new BenchmarkResult(bf, kd, hn, items.size());
    }

    private long time(Runnable r) {
        long start = System.nanoTime();
        r.run();
        return (System.nanoTime() - start) / 1000;
    }

    /** @return all stored items in insertion order. */
    public synchronized List<VectorItem> all() {
        return new ArrayList<>(items.values());
    }

    /** @return the HNSW graph structure for the visualizer. */
    public synchronized HnswIndex.GraphInfo hnswInfo() {
        return hnsw.info();
    }

    /** @return number of stored demo vectors. */
    public synchronized int size() {
        return items.size();
    }
}
