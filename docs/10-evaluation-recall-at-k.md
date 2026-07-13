# 10. Evaluation — recall@k

## What it is (simple analogy)

**recall@k** asks: *of the truly best k answers, how many did my fast search actually find?*

Imagine the 5 nearest restaurants really are A, B, C, D, E. Your fast app returns A, B, C, D, X.
It found 4 of the 5 right ones → **recall@5 = 4/5 = 0.8 (80%)**.

## Why it exists

HNSW is **approximate** — it trades a little accuracy for a lot of speed. "A little" is not a
feeling; it's a number you must **measure**. Building an index without measuring recall is like
shipping code without tests. The exact **brute-force** result is the ground truth we compare
against (brute force is always right — see doc 4).

## How it works here

For every demo vector used as a query, we compute the exact top-k (brute force) and the
approximate top-k (KD-Tree, HNSW), then measure the overlap:

```62:78:src/main/java/com/learn/vectordb/eval/EvaluationService.java
                Set<Integer> exactIds = idSet(exact.results());
                if (exactIds.isEmpty()) {
                    continue;
                }
                Set<Integer> approxIds = idSet(approx.results());
                int overlap = 0;
                for (Integer id : approxIds) {
                    if (exactIds.contains(id)) {
                        overlap++;
                    }
                }
                recallSum += (double) overlap / exactIds.size();
```

Try it live: **"▶ MEASURE RECALL"** in the UI, or `GET /eval/recall?k=5&metric=cosine`.
KD-Tree is exact so it scores 1.0; HNSW scores high (our test asserts ≥ 0.8). The report also
includes average query latency, so you can see the speed/accuracy trade-off in one place.

## How to remember it

> **recall@k = (correct hits found) / k. Higher is better. Brute force is the answer key.**

Related terms: **precision@k** (of what I returned, how much was relevant) and **latency**
(how fast). recall vs latency is the core ANN trade-off.

## Where it shows up in real life / interviews

- Tuning any ANN index (HNSW `ef`, IVF `nprobe`) is literally "raise `ef` until recall is high
  enough, keep latency acceptable." Now you can talk about that from experience.
- Strong interview signal: *"I don't just build the index; I measure recall@k against exact
  search and tune parameters to hit a recall/latency target."* Very few juniors say this.
