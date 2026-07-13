# 3. Distance Metrics — Measuring "Closeness"

## What it is (simple analogy)

A **distance metric** is a rule for measuring how far apart two vectors are. **Smaller distance
= more similar.** This project supports three.

| Metric | Everyday analogy | Formula (idea) |
|---|---|---|
| **Euclidean** | straight line "as the crow flies" | sqrt(sum of squared differences) |
| **Manhattan** | walking a city grid, block by block | sum of absolute differences |
| **Cosine** | are two arrows pointing the same way? | 1 − (angle similarity), ignores length |

## Why it exists

"Closeness" is not one fixed thing. If only *direction* matters (which is usually true for text
embeddings), you want **cosine**. If *magnitude* matters (e.g. physical measurements), you want
**euclidean**. Choosing the right metric changes your results.

## How it works here

All three are in one enum, each implementing a single method:

```37:52:src/main/java/com/learn/vectordb/distance/DistanceMetric.java
    COSINE {
        @Override
        public float distance(float[] a, float[] b) {
            float dot = 0f, normA = 0f, normB = 0f;
            for (int i = 0; i < a.length; i++) {
                dot += a[i] * b[i];
                normA += a[i] * a[i];
                normB += b[i] * b[i];
            }
```

Text embeddings in the `DocumentStore` always use **cosine** — the standard choice. The demo tab
lets you switch metrics live and watch results change.

## How to remember it

> **Cosine = same direction? Euclidean = same spot? Manhattan = grid blocks apart?**
> For text, reach for **cosine** first.

Memory hook: **C**osine for **C**ontent/meaning.

## Where it shows up in real life / interviews

- Pinecone/Weaviate/FAISS all ask you to pick a metric (cosine, dot product, or L2/euclidean)
  when you create an index. Picking wrong quietly wrecks quality.
- Interview nuance: on **normalized** vectors (length 1), cosine distance and euclidean distance
  rank results identically — a neat fact that shows you understand the math, not just the API.
