# 5. KD-Tree

## What it is (simple analogy)

A **KD-Tree** organizes points by repeatedly **splitting space in half**, one dimension at a
time. Like the game "20 questions": "Is dimension 0 above or below this value?" Each answer
throws away half the remaining space, so you zoom in fast.

## Why it exists

Brute force checks everyone. A KD-Tree lets you **skip** (prune) large groups of points that
can't possibly be closer than what you already found — so on average it's much faster than
brute force **in low dimensions**, while staying **exact**.

## How it works here

Insertion cycles through dimensions by depth (dimension = depth % dims):

```55:65:src/main/java/com/learn/vectordb/index/KdTreeIndex.java
        int axis = depth % dims;
        if (item.embedding()[axis] < node.item.embedding()[axis]) {
            node.left = insert(node.left, item, depth + 1);
        } else {
            node.right = insert(node.right, item, depth + 1);
        }
        return node;
```

Search visits the closer side first, then only visits the far side **if** it could still contain
a better neighbor (the "ball within hyperslab" check). See the `knn` method.

## The catch: the curse of dimensionality

KD-Trees shine at ~2–20 dimensions. At high dimensions (like 768-D embeddings) almost every
point looks equally far, so the pruning check almost never lets you skip anything — and the tree
degrades to brute-force speed. **This is the single most important reason real vector DBs use
HNSW instead of KD-Trees for embeddings.**

Our test proves KD-Tree is exact by matching brute force:
`src/test/java/com/learn/vectordb/index/IndexCorrectnessTest.java`.

## How to remember it

> **KD-Tree = 20-questions on coordinates. Fast and exact in low dimensions, useless in high
> dimensions.**

## Where it shows up in real life / interviews

- Classic use: 2D/3D spatial data — nearest gas station, k-NN classifiers on small feature sets,
  game collision/proximity.
- Interview gold: being able to explain *why* KD-Trees fail for embeddings (curse of
  dimensionality) and what to use instead (HNSW) shows real depth.
