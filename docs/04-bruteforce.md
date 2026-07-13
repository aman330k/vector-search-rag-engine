# 4. Brute Force Search

## What it is (simple analogy)

**Check everything.** To find the closest vector to your query, compare the query against
*every* stored vector, then sort. Like finding the tallest person in a room by measuring
everyone one by one.

## Why it exists

- It is **always 100% correct** (it literally checks all candidates).
- It is dead simple, so it is the perfect **ground truth** to measure smarter (but approximate)
  algorithms against. Our evaluation harness uses brute force as "the right answer".

The downside: it is **slow** — cost grows linearly with the number of items (**O(N·d)** for N
items of d dimensions). Fine for thousands, painful for millions.

## How it works here

```28:38:src/main/java/com/learn/vectordb/index/BruteForceIndex.java
    @Override
    public List<Neighbor> knn(float[] query, int k, DistanceMetric metric) {
        List<Neighbor> results = new ArrayList<>(items.size());
        for (VectorItem item : items) {
            results.add(new Neighbor(metric.distance(query, item.embedding()), item.id()));
        }
        Collections.sort(results);
```

Compute distance to all, sort ascending, keep the top `k`. That is the whole algorithm.

## How to remember it

> **Brute force = compare with everyone. Perfectly accurate, gets slow as data grows.**

## Where it shows up in real life / interviews

- It is the baseline every ANN (Approximate Nearest Neighbor) library benchmarks against.
- "Exact search" mode in FAISS (`IndexFlatL2`) is exactly this.
- Interview point: for small datasets (say < 10k vectors) brute force is often the *right*
  choice — no index to build, no accuracy loss. Don't over-engineer. (Our `DocumentStore` even
  falls back to brute force while it has fewer than 10 chunks.)
