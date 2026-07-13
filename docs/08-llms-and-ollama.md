# 8. LLMs and Ollama

## What it is (simple analogy)

An **LLM (Large Language Model)** like `llama3.2` is a very well-read autocomplete. Given some
text, it predicts the next words, over and over, to produce fluent answers. It learned patterns
from a huge amount of text during training.

**Ollama** is a free app that runs these models **locally on your Mac** — no cloud, no API key,
no data leaving your machine. It exposes a small HTTP API on `http://127.0.0.1:11434`.

## Why it exists (why local?)

- **Privacy:** your documents never leave your laptop.
- **Free & offline:** no per-token billing.
- **Great for learning:** you see the whole pipeline end to end.

We use **two** models:

| Model | Job | Output |
|---|---|---|
| `nomic-embed-text` | **Embedding** — text → vector | 768 numbers |
| `llama3.2` | **Generation** — prompt → answer | text |

## How it works here

Our `OllamaClient` is a thin wrapper over two endpoints using the JDK's built-in HTTP client
(no extra dependency):

```121:145:src/main/java/com/learn/vectordb/embedding/OllamaClient.java
    public String generate(String prompt) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", config.getGenModel());
            body.put("prompt", prompt);
            body.put("stream", false);
```

Everything is configurable in `application.yml` (`vectordb.ollama.*`) — base URL, model names,
and timeouts. Generation gets a long timeout (180s) because LLMs can be slow on a laptop CPU.

If Ollama is offline, the app still runs; the demo/search tabs work, and RAG features return a
friendly "Ollama unavailable" message instead of crashing.

## How to remember it

> **LLM = smart autocomplete. Ollama = the app that runs it on your own machine.**
> Two models: one to *embed* (nomic-embed-text), one to *answer* (llama3.2).

## Where it shows up in real life / interviews

- Production RAG uses the same split: an embedding model + a generation model. The names change
  (OpenAI `text-embedding-3`, `gpt-4o`; Cohere; etc.) but the roles are identical.
- Interview points:
  - *"What's the context window?"* → the max text an LLM can read at once; it's why we chunk and
    retrieve only top-k instead of stuffing everything.
  - *"Temperature?"* → randomness knob for generation (higher = more creative/varied).
  - *"Why a smaller model?"* → `llama3.2:1b` answers faster on weak hardware; speed vs quality
    trade-off.
