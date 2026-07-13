# 1. What Is a Vector?

## What it is (simple analogy)

A **vector** is just an ordered **list of numbers**. That's it.

```
[0.90, 0.85, 0.12, 0.08]
```

Think of a location on a map: `(latitude, longitude)` is a 2-number vector. A vector can have 2
numbers, 16 numbers, or 768 numbers. Each number is called a **dimension**.

In this project, the demo vectors have **16 dimensions** and the real text embeddings from
Ollama have **768 dimensions**.

## Why it exists

Computers can't compare "sushi" and "ramen" as words directly. But if we turn each into a
vector of numbers, we can do **math** on them — measure how far apart they are, add them,
average them. Vectors are the bridge between messy human data (text, images) and math.

## How it works here

Every stored item is a vector plus some labels. See the record:

```12:12:src/main/java/com/learn/vectordb/model/VectorItem.java
public record VectorItem(int id, String metadata, String category, float[] embedding) {
```

The demo data is hand-built so each **category** lives in its own block of dimensions
(0-3 = CS, 4-7 = Math, 8-11 = Food, 12-15 = Sports). That is why items in the same category end
up close together and form clusters on the scatter plot. See
`src/main/java/com/learn/vectordb/store/DemoDataLoader.java`.

## How to remember it

> **Vector = a point in space described by a list of numbers. More similar things sit closer
> together.**

Picture stars in the sky: each star is a point; similar stars cluster.

## Where it shows up in real life / interviews

- Every "similarity" feature you've used — Spotify "songs like this", Amazon "related items",
  Google image search — is comparing vectors under the hood.
- Interview phrasing: *"Embeddings map objects into a high-dimensional vector space where
  distance encodes semantic similarity."* Now you know exactly what that sentence means.
