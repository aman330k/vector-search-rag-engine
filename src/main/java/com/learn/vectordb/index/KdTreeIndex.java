package com.learn.vectordb.index;

import com.learn.vectordb.distance.DistanceMetric;
import com.learn.vectordb.model.Neighbor;
import com.learn.vectordb.model.VectorItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A KD-Tree (K-Dimensional Tree): binary space partitioning for exact nearest-neighbor search.
 *
 * <p>Each node splits space along one dimension, cycling through dimensions by depth. Search
 * can prune whole subtrees when the closest
 * possible point in a subtree cannot beat the current worst-of-k ("ball within hyperslab").
 *
 * <p>Great for low dimensions (<= ~20). At high dimensions it degrades toward brute force
 * (the "curse of dimensionality"), which is exactly why HNSW is used for real embeddings.
 */
public class KdTreeIndex implements VectorIndex {

    private static final class Node {
        final VectorItem item;
        Node left;
        Node right;

        Node(VectorItem item) {
            this.item = item;
        }
    }

    private final int dims;
    private Node root;
    private int count;

    public KdTreeIndex(int dims) {
        this.dims = dims;
    }

    @Override
    public void insert(VectorItem item, DistanceMetric metric) {
        root = insert(root, item, 0);
        count++;
    }

    private Node insert(Node node, VectorItem item, int depth) {
        if (node == null) {
            return new Node(item);
        }
        int axis = depth % dims;
        if (item.embedding()[axis] < node.item.embedding()[axis]) {
            node.left = insert(node.left, item, depth + 1);
        } else {
            node.right = insert(node.right, item, depth + 1);
        }
        return node;
    }

    @Override
    public List<Neighbor> knn(float[] query, int k, DistanceMetric metric) {
        // Max-heap keyed by distance: the root is the current worst of the best k.
        PriorityQueue<Neighbor> heap = new PriorityQueue<>(Comparator.reverseOrder());
        knn(root, query, k, 0, metric, heap);
        List<Neighbor> results = new ArrayList<>(heap);
        Collections.sort(results);
        return results;
    }

    private void knn(Node node, float[] query, int k, int depth,
                     DistanceMetric metric, PriorityQueue<Neighbor> heap) {
        if (node == null) {
            return;
        }
        float d = metric.distance(query, node.item.embedding());
        if (heap.size() < k || d < heap.peek().distance()) {
            heap.offer(new Neighbor(d, node.item.id()));
            if (heap.size() > k) {
                heap.poll();
            }
        }
        int axis = depth % dims;
        float diff = query[axis] - node.item.embedding()[axis];
        Node closer = diff < 0 ? node.left : node.right;
        Node farther = diff < 0 ? node.right : node.left;
        knn(closer, query, k, depth + 1, metric, heap);
        if (heap.size() < k || Math.abs(diff) < heap.peek().distance()) {
            knn(farther, query, k, depth + 1, metric, heap);
        }
    }

    @Override
    public void remove(int id) {
        // KD-Trees do not support cheap deletion; the store rebuilds after removals.
        List<VectorItem> remaining = new ArrayList<>();
        collect(root, remaining);
        rebuild(remaining, id);
    }

    private void collect(Node node, List<VectorItem> out) {
        if (node == null) {
            return;
        }
        out.add(node.item);
        collect(node.left, out);
        collect(node.right, out);
    }

    private void rebuild(List<VectorItem> items, int excludeId) {
        root = null;
        count = 0;
        for (VectorItem item : items) {
            if (item.id() != excludeId) {
                insert(item, null);
            }
        }
    }

    /**
     * Rebuilds the whole tree from a fresh list of items (used by the store after deletes).
     *
     * @param items the complete set of items to index
     */
    public void rebuild(List<VectorItem> items) {
        root = null;
        count = 0;
        for (VectorItem item : items) {
            insert(item, null);
        }
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public String name() {
        return "kdtree";
    }
}
