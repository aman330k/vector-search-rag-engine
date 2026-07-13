# VectorDB-Java — Build a Vector Database + RAG from Scratch (Java / Spring Boot)

A fully working **vector database** and **RAG** system, hand-written in Java on Spring Boot.
It implements **HNSW**, **KD-Tree**, and **Brute Force** nearest-neighbor search side by side,
three distance metrics, metadata filtering, disk persistence, a recall@k evaluation harness, and
a browser UI — plus a full **RAG pipeline** powered by a local LLM via **Ollama**.

> This project is built to **learn AI, vector search, and RAG from the ground up**. Every concept
> is explained in plain language in the [`docs/`](docs/00-START-HERE.md) folder. Start there if
> you're new to AI.

Tested on **macOS** (Apple Silicon, including M1/M2/M3).

---

## What this project does

| Feature | Description |
|---|---|
| **3 search algorithms** | HNSW (production-grade), KD-Tree, Brute Force — run all three and compare |
| **3 distance metrics** | Cosine, Euclidean, Manhattan |
| **16-D demo vectors** | 20 pre-loaded vectors across 4 categories (CS, Math, Food, Sports) |
| **2D PCA scatter plot** | Live visualization of the semantic space — watch clusters form |
| **Real embeddings** | Paste text → Ollama embeds it with `nomic-embed-text` (768-D) |
| **RAG pipeline** | Ask questions → HNSW retrieves context → local LLM answers |
| **Metadata filtering** *(new)* | Restrict search to a category |
| **Disk persistence** *(new)* | Documents survive restarts (JSON snapshot) |
| **recall@k evaluation** *(new)* | Measure HNSW/KD-Tree accuracy vs exact search |
| **Sentence-aware chunking** *(new)* | Cleaner chunks for better retrieval |
| **Full REST API** | Search, insert, delete, benchmark, hnsw-info, docs, RAG, admin, eval |

---

## How it works

```
Your Text
    │  ▼
Ollama (nomic-embed-text)   ← turns text into a 768-D vector
    │  ▼
HNSW Index (Java)           ← indexes the vector in a multilayer graph
    │  ▼
Semantic Search             ← finds nearest neighbors in vector space
    │  ▼
Ollama (llama3.2:1b)        ← reads retrieved chunks, generates an answer
    │  ▼
Answer
```

New to any of these words? Read [`docs/00-START-HERE.md`](docs/00-START-HERE.md).

---

## Prerequisites (macOS)

You need **3 things**: a JDK (17+), Maven, and Ollama. The easiest way is [Homebrew](https://brew.sh).

### Step 1 — Install Homebrew (if you don't have it)

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

### Step 2 — Install a JDK and Maven

```bash
brew install openjdk@21 maven
```

Verify:

```bash
java -version    # should print 17 or newer
mvn -version
```

> If `java -version` shows an old version, point your shell at the Homebrew JDK:
> `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`

### Step 3 — Install Ollama and pull the models

```bash
brew install ollama              # or download the app from https://ollama.com
ollama pull nomic-embed-text     # ~274 MB — the embedding model
ollama pull llama3.2:1b          # ~1.3 GB — the language model
```

Verify:

```bash
ollama list                  # should show both models
```

> **Why `llama3.2:1b` and not the full `llama3.2` (3B)?** This project defaults to the **1B**
> variant because it is noticeably faster on a laptop — a great fit for machines like an
> **M2 MacBook** running on CPU/unified memory, with only a small quality trade-off for a
> learning project. If you have plenty of RAM (16 GB+) and want better answer quality, you can
> switch to the bigger model: `ollama pull llama3.2` and set
> `vectordb.ollama.gen-model: llama3.2` in `src/main/resources/application.yml`.

---

## Run it

**Terminal 1 — start Ollama** (skip if the menu-bar app is already running):

```bash
ollama serve
```

**Terminal 2 — start the app:**

```bash
./run.sh
```

`run.sh` just runs `mvn spring-boot:run`. When it boots you'll see:

```
=== VectorDB Engine (Java) ===
http://localhost:8081
20 demo vectors | 16 dims | HNSW+KD-Tree+BruteForce
Ollama: ONLINE
  embed model: nomic-embed-text  gen model: llama3.2:1b
```

Open **http://localhost:8081** in your browser.

> **Why 8081, not the more common 8080?** So this app doesn't collide with other local
> services (many dev servers/APIs default to 8080). Change it back via `server.port` in
> `src/main/resources/application.yml` if you'd rather use 8080 and nothing else needs it.

> First run downloads Maven dependencies — give it a minute.

### One-command setup helper

```bash
./setup-mac.sh   # checks/install prerequisites via Homebrew and pulls the models
```

---

## Using the app

- **Search tab:** type a concept (`binary tree`, `sushi`, `basketball`), pick an algorithm and
  metric, hit SEARCH. Use **Category Filter** to restrict results. Click **COMPARE ALL ALGOS**
  to benchmark, and **MEASURE RECALL** to score HNSW/KD-Tree vs exact search.
- **Documents tab:** paste text, click EMBED & INSERT. Long text is auto-chunked and each chunk
  gets a real 768-D embedding. Use **SAVE / LOAD** (left panel) to persist documents to disk.
- **Ask AI tab:** ask a question about your inserted documents — the RAG pipeline retrieves
  context and the local LLM answers, showing which chunks it used.

---

## REST API

| Method | Endpoint | Description |
|---|---|---|
| GET | `/search?v=..&k=5&metric=cosine&algo=hnsw&category=cs` | k-NN search (optional category filter) |
| POST | `/insert` | Insert a demo vector `{metadata, category, embedding}` |
| DELETE | `/delete/{id}` | Delete a demo vector |
| GET | `/items` | List demo vectors |
| GET | `/benchmark?v=..&k=5&metric=cosine` | Time all 3 algorithms |
| GET | `/hnsw-info` | HNSW graph structure |
| GET | `/stats` | Database stats |
| POST | `/doc/insert` | Embed + store a document `{title, text}` |
| GET | `/doc/list` | List stored chunks |
| DELETE | `/doc/delete/{id}` | Delete a chunk |
| POST | `/doc/search` | Retrieve-only `{question, k}` |
| POST | `/doc/ask` | Full RAG `{question, k}` |
| GET | `/status` | Ollama status + model info |
| POST | `/admin/save` *(new)* | Save documents to disk |
| POST | `/admin/load` *(new)* | Load documents from disk |
| GET | `/eval/recall?k=5&metric=cosine` *(new)* | recall@k for KD-Tree & HNSW vs brute force |

### Example: search via curl

```bash
curl "http://localhost:8081/search?v=0.9,0.8,0.7,0.6,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1&k=3&metric=cosine&algo=hnsw"
```

### Example: ask a question via curl

```bash
curl -X POST http://localhost:8081/doc/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"What is dynamic programming?","k":3}'
```

---

## Project structure

```
vectordb-java/
├── pom.xml
├── run.sh                 ← start the app (mvn spring-boot:run)
├── setup-mac.sh           ← install prerequisites on macOS
├── Dockerfile             ← containerize the app
├── docker-compose.yml     ← app + Ollama together
├── docs/                  ← plain-language learning material (START HERE)
└── src/
    ├── main/java/com/learn/vectordb/
    │   ├── distance/      ← DistanceMetric (cosine/euclidean/manhattan)
    │   ├── index/         ← VectorIndex + BruteForce / KdTree / Hnsw
    │   ├── store/         ← VectorStore, DocumentStore, persistence, demo data
    │   ├── embedding/     ← OllamaClient
    │   ├── rag/           ← TextChunker, RagService, DocumentIngestService
    │   ├── eval/          ← EvaluationService (recall@k)
    │   ├── web/           ← REST controllers + DTOs + CORS
    │   └── config/        ← VectorDbProperties
    ├── main/resources/
    │   ├── application.yml
    │   └── static/index.html   ← the browser UI
    └── test/java/...      ← JUnit 5 tests
```

---

## Testing

```bash
./run.sh                 # or: mvn spring-boot:run
mvn test                 # run all unit + controller-slice tests
```

Tests cover distance metrics, each index vs a brute-force oracle, chunking edge cases, recall@k,
and the REST controllers (with Ollama mocked, so **no Ollama install is needed to run tests**).

---

## Common issues (macOS)

| Problem | Fix |
|---|---|
| `Ollama: OFFLINE` in the banner | Run `ollama serve`, or open the Ollama menu-bar app |
| Embedding takes forever the first time | Ollama is loading the model; wait ~1–2 min |
| `java -version` shows Java 8 | `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` |
| Port 8081 already in use | `lsof -i :8081` then `kill <PID>` |
| LLM answer is slow | Normal on CPU. The default `llama3.2:1b` is already the fast option; avoid switching to the bigger `llama3.2` (3B) unless you have RAM to spare |

---

## License

MIT — use this however you want. Built for learning.
