package com.learn.vectordb.index;

import com.learn.vectordb.distance.DistanceMetric;
import com.learn.vectordb.model.Neighbor;
import com.learn.vectordb.model.VectorItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

/**
 * HNSW — Hierarchical Navigable Small World graph.
 *
 * <p>This is the same family of algorithm used by production vector databases (Pinecone,
 * Weaviate, Chroma, Milvus). It builds a multilayer graph: the top layers are sparse
 * "highways" that get you into the right neighborhood fast, and layer 0 holds every node with
 * dense connections for the final fine-grained search. Search cost is roughly O(log N) instead
 * of brute force's O(N).
 *
 * <p>Key parameters:
 * <ul>
 *   <li>{@code M} — max neighbors per node on upper layers (default 16)</li>
 *   <li>{@code M0} — max neighbors on layer 0 (2*M)</li>
 *   <li>{@code efConstruction} — beam width while building (default 200)</li>
 *   <li>{@code mL} — level-generation factor 1/ln(M)</li>
 * </ul>
 */
public class HnswIndex implements VectorIndex {

    private static final class Node {
        final VectorItem item;
        final int maxLayer;
        final List<List<Integer>> neighbors;

        Node(VectorItem item, int maxLayer) {
            this.item = item;
            this.maxLayer = maxLayer;
            this.neighbors = new ArrayList<>(maxLayer + 1);
            for (int i = 0; i <= maxLayer; i++) {
                this.neighbors.add(new ArrayList<>());
            }
        }
    }

    private final Map<Integer, Node> graph = new HashMap<>();
    private final int m;
    private final int m0;
    private final int efConstruction;
    private final double mL;
    private final Random rng;

    private int topLayer = -1;
    private int entryPoint = -1;

    public HnswIndex() {
        this(16, 200);
    }

    public HnswIndex(int m, int efConstruction) {
        this.m = m;
        this.m0 = 2 * m;
        this.efConstruction = efConstruction;
        this.mL = 1.0 / Math.log(m);
        // Fixed seed keeps graph construction deterministic (helpful for tests).
        this.rng = new Random(42);
    }

    private int randomLevel() {
        double u = rng.nextDouble();
        if (u <= 0.0) {
            u = Double.MIN_VALUE;
        }
        return (int) Math.floor(-Math.log(u) * mL);
    }

    /**
     * Greedy beam search within a single layer, returning the ef closest candidates.
     * This is the heart of both insertion and query.
     */
    private List<Neighbor> searchLayer(float[] query, int entry, int ef, int layer,
                                       DistanceMetric metric) {
        Set<Integer> visited = new HashSet<>();
        // Candidates to explore next: min-heap (closest first).
        PriorityQueue<Neighbor> candidates = new PriorityQueue<>();
        // Best results found so far: max-heap (worst on top for easy eviction).
        PriorityQueue<Neighbor> found = new PriorityQueue<>(Comparator.reverseOrder());

        float d0 = metric.distance(query, graph.get(entry).item.embedding());
        visited.add(entry);
        candidates.offer(new Neighbor(d0, entry));
        found.offer(new Neighbor(d0, entry));

        while (!candidates.isEmpty()) {
            Neighbor current = candidates.poll();
            if (found.size() >= ef && current.distance() > found.peek().distance()) {
                break;
            }
            Node currentNode = graph.get(current.id());
            if (currentNode == null || layer >= currentNode.neighbors.size()) {
                continue;
            }
            for (int neighborId : currentNode.neighbors.get(layer)) {
                if (visited.contains(neighborId) || !graph.containsKey(neighborId)) {
                    continue;
                }
                visited.add(neighborId);
                float nd = metric.distance(query, graph.get(neighborId).item.embedding());
                if (found.size() < ef || nd < found.peek().distance()) {
                    candidates.offer(new Neighbor(nd, neighborId));
                    found.offer(new Neighbor(nd, neighborId));
                    if (found.size() > ef) {
                        found.poll();
                    }
                }
            }
        }

        List<Neighbor> result = new ArrayList<>(found);
        Collections.sort(result);
        return result;
    }

    private List<Integer> selectNeighbors(List<Neighbor> candidates, int maxM) {
        List<Integer> selected = new ArrayList<>();
        int limit = Math.min(candidates.size(), maxM);
        for (int i = 0; i < limit; i++) {
            selected.add(candidates.get(i).id());
        }
        return selected;
    }

    @Override
    public void insert(VectorItem item, DistanceMetric metric) {
        int id = item.id();
        int level = randomLevel();
        graph.put(id, new Node(item, level));

        if (entryPoint == -1) {
            entryPoint = id;
            topLayer = level;
            return;
        }

        int ep = entryPoint;
        float[] emb = item.embedding();

        // Descend from the top layer to just above our assigned level, greedily.
        for (int lc = topLayer; lc > level; lc--) {
            if (lc < graph.get(ep).neighbors.size()) {
                List<Neighbor> w = searchLayer(emb, ep, 1, lc, metric);
                if (!w.isEmpty()) {
                    ep = w.get(0).id();
                }
            }
        }

        // From our level down to 0, connect to the M (or M0) nearest neighbors.
        for (int lc = Math.min(topLayer, level); lc >= 0; lc--) {
            List<Neighbor> w = searchLayer(emb, ep, efConstruction, lc, metric);
            int maxM = (lc == 0) ? m0 : m;
            List<Integer> selected = selectNeighbors(w, maxM);
            graph.get(id).neighbors.set(lc, new ArrayList<>(selected));

            for (int neighborId : selected) {
                Node neighborNode = graph.get(neighborId);
                if (neighborNode == null) {
                    continue;
                }
                while (neighborNode.neighbors.size() <= lc) {
                    neighborNode.neighbors.add(new ArrayList<>());
                }
                List<Integer> conn = neighborNode.neighbors.get(lc);
                conn.add(id);
                if (conn.size() > maxM) {
                    // Keep only the maxM closest connections (bidirectional pruning).
                    List<Neighbor> scored = new ArrayList<>();
                    for (int c : conn) {
                        if (graph.containsKey(c)) {
                            scored.add(new Neighbor(
                                    metric.distance(neighborNode.item.embedding(),
                                            graph.get(c).item.embedding()), c));
                        }
                    }
                    Collections.sort(scored);
                    conn.clear();
                    for (int i = 0; i < maxM && i < scored.size(); i++) {
                        conn.add(scored.get(i).id());
                    }
                }
            }
            if (!w.isEmpty()) {
                ep = w.get(0).id();
            }
        }

        if (level > topLayer) {
            topLayer = level;
            entryPoint = id;
        }
    }

    @Override
    public List<Neighbor> knn(float[] query, int k, DistanceMetric metric) {
        return knn(query, k, 50, metric);
    }

    /**
     * k-NN query with an explicit search-width (ef) knob. Larger ef means more accurate but
     * slower search.
     *
     * @param query  query vector
     * @param k      number of neighbors to return
     * @param ef     beam width at layer 0
     * @param metric distance metric
     * @return up to k neighbors, closest first
     */
    public List<Neighbor> knn(float[] query, int k, int ef, DistanceMetric metric) {
        if (entryPoint == -1) {
            return new ArrayList<>();
        }
        int ep = entryPoint;
        for (int lc = topLayer; lc > 0; lc--) {
            if (lc < graph.get(ep).neighbors.size()) {
                List<Neighbor> w = searchLayer(query, ep, 1, lc, metric);
                if (!w.isEmpty()) {
                    ep = w.get(0).id();
                }
            }
        }
        List<Neighbor> w = searchLayer(query, ep, Math.max(ef, k), 0, metric);
        if (w.size() > k) {
            return new ArrayList<>(w.subList(0, k));
        }
        return w;
    }

    @Override
    public void remove(int id) {
        if (!graph.containsKey(id)) {
            return;
        }
        for (Node node : graph.values()) {
            for (List<Integer> layer : node.neighbors) {
                layer.removeIf(nid -> nid == id);
            }
        }
        if (entryPoint == id) {
            entryPoint = -1;
            for (Integer nid : graph.keySet()) {
                if (nid != id) {
                    entryPoint = nid;
                    break;
                }
            }
        }
        graph.remove(id);
    }

    @Override
    public int size() {
        return graph.size();
    }

    @Override
    public String name() {
        return "hnsw";
    }

    /**
     * Snapshot of the graph structure for the {@code /hnsw-info} visualizer.
     *
     * @return an immutable-ish view describing layers, nodes and edges
     */
    public GraphInfo info() {
        GraphInfo gi = new GraphInfo();
        gi.topLayer = topLayer;
        gi.nodeCount = graph.size();
        int maxLayers = Math.max(topLayer + 1, 1);
        gi.nodesPerLayer = new int[maxLayers];
        gi.edgesPerLayer = new int[maxLayers];
        gi.nodes = new ArrayList<>();
        gi.edges = new ArrayList<>();

        for (Map.Entry<Integer, Node> entry : graph.entrySet()) {
            int id = entry.getKey();
            Node node = entry.getValue();
            gi.nodes.add(new GraphInfo.NodeView(id, node.item.metadata(),
                    node.item.category(), node.maxLayer));
            for (int lc = 0; lc <= node.maxLayer && lc < maxLayers; lc++) {
                gi.nodesPerLayer[lc]++;
                if (lc < node.neighbors.size()) {
                    for (int neighborId : node.neighbors.get(lc)) {
                        if (id < neighborId) {
                            gi.edgesPerLayer[lc]++;
                            gi.edges.add(new GraphInfo.EdgeView(id, neighborId, lc));
                        }
                    }
                }
            }
        }
        return gi;
    }

    /** Plain data holder describing the HNSW graph for visualization. */
    public static final class GraphInfo {
        public int topLayer;
        public int nodeCount;
        public int[] nodesPerLayer;
        public int[] edgesPerLayer;
        public List<NodeView> nodes;
        public List<EdgeView> edges;

        public record NodeView(int id, String metadata, String category, int maxLyr) {
        }

        public record EdgeView(int src, int dst, int lyr) {
        }
    }
}
