# 9. Chunking — Splitting Documents Well

## What it is (simple analogy)

**Chunking** = cutting a long document into bite-sized pieces before embedding. Like slicing a
loaf of bread so each slice fits in the toaster — you can't toast the whole loaf at once.

## Why it exists

Three reasons:

1. **Model limits:** embedding models have a maximum input size.
2. **Precision:** a small chunk about *one idea* embeds into a sharp, meaningful vector. A whole
   50-page document averages into mush ("what is this about? …everything").
3. **Better answers:** retrieving a tight, relevant paragraph beats retrieving a whole chapter.

**Overlap** matters too: if we cut exactly at boundaries, a sentence split across two chunks
loses meaning. So consecutive chunks share a few words at the seam.

## How it works here — two strategies

```71:78:src/main/java/com/learn/vectordb/rag/TextChunker.java
    public List<String> chunk(String text, String strategy, int chunkWords, int overlapWords) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }
        if ("sentence".equalsIgnoreCase(strategy)) {
            return chunkBySentence(text, chunkWords);
        }
        return chunkByWord(text, chunkWords, overlapWords);
    }
```

- **word** (default): fixed windows of ~250 words with ~30 words of overlap. Simple and
  predictable.
- **sentence** (new): packs whole sentences up to a word budget so a chunk **never ends
  mid-sentence** — usually cleaner retrieval.

Configure defaults in `application.yml` under `vectordb.chunking`. Tests cover the overlap math
and the infinite-loop guard: `src/test/java/com/learn/vectordb/rag/TextChunkerTest.java`.

## How to remember it

> **Chunk = toast-sized slices of text, with a little overlap so nothing gets cut in half.**

## Where it shows up in real life / interviews

- Chunking quality is *the* most common reason a RAG system gives bad answers. Interviewers love
  asking "your RAG bot is returning irrelevant stuff — where do you look first?" → chunking and
  retrieval.
- Real systems tune chunk size (often 200–500 tokens), overlap (10–20%), and increasingly use
  **semantic / structure-aware** chunking (split on headings, code blocks, sentences) — exactly
  the direction our "sentence" strategy points toward.
