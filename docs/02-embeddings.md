# 2. Embeddings — Turning Text Into Vectors

## What it is (simple analogy)

An **embedding** is a vector that captures the *meaning* of something. An **embedding model**
is the AI that produces it.

Analogy: imagine a giant library where every book is placed on a shelf by topic. Books about
cooking sit near each other; books about math sit far away in another wing. An embedding is the
"shelf coordinates" of a piece of text — and texts with similar meaning get similar coordinates.

## Why it exists

The magic property: **similar meaning → similar vector**, even when the words differ.
"car" and "automobile" land close together; "car" and "banana" land far apart. This lets us
search by *meaning* ("semantic search") instead of exact keywords.

## How it works here

We do not train an embedding model ourselves — that takes huge data and compute. Instead we ask
a local model (`nomic-embed-text`, run by Ollama) to embed text for us:

```96:118:src/main/java/com/learn/vectordb/embedding/OllamaClient.java
    public float[] embed(String text) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", config.getEmbedModel());
            body.put("prompt", text);
```

The model returns a 768-number vector. We store that in the `DocumentStore`. For the *demo* tab,
we instead use tiny hand-made 16-D vectors so you can see the whole thing on a 2D plot.

## How to remember it

> **Embedding = meaning as coordinates. The embedding model is a translator from text to a point
> in space.**

## Where it shows up in real life / interviews

- Semantic search, recommendation, clustering, deduplication, RAG — all start with embeddings.
- Common interview question: *"Why not just keyword match?"* Answer: keywords miss synonyms and
  paraphrases; embeddings capture meaning, so "How do I reset my password?" matches a doc titled
  "Account recovery steps".
- Know the trade-off: embeddings need a model and are approximate; keyword search (BM25) is
  exact and cheap. Real systems often combine both ("hybrid search").
