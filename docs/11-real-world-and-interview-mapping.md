# 11. Real-World & Interview Mapping

This project is small on purpose, but every piece maps directly to production systems and common
interview questions. Use this page as your "so what?" cheat sheet.

## This project vs the real tools

| What you built here | The production equivalent |
|---|---|
| `HnswIndex` | The HNSW index inside **Pinecone, Weaviate, Chroma, Milvus, Qdrant, FAISS** |
| `DistanceMetric` (cosine/L2/L1) | The "metric" you pick when creating an index anywhere |
| `VectorStore` / `DocumentStore` | A vector database collection / namespace |
| Metadata **category filter** | Pinecone metadata filters, Weaviate `where`, Qdrant payload filters |
| `OllamaClient` (embed + generate) | OpenAI / Cohere / Bedrock embedding + chat APIs |
| `RagService` | LangChain / LlamaIndex "RetrievalQA" chains |
| `TextChunker` | LangChain `RecursiveCharacterTextSplitter` & friends |
| `SnapshotRepository` (persistence) | The DB's on-disk storage / durability layer |
| `EvaluationService` (recall@k) | Ragas, TruLens, FAISS benchmark scripts |

## Feature-by-feature "why it matters"

- **Persistence** (`/admin/save`, `/admin/load`): without it, a restart loses everything, since
  the store lives in memory. Real databases must survive restarts. Shows you think about
  **durability**.
- **Metadata filtering**: "search only in category = cs". Real apps constantly need "search only
  this user's docs / this tenant / this language". Shows you understand **filtered vector
  search** (and its pre-filter vs post-filter trade-off — see below).
- **Sentence-aware chunking**: better retrieval quality. Shows you know retrieval quality starts
  with **good chunks**.
- **recall@k evaluation**: shows you **measure**, not just build.

## Pre-filter vs post-filter (a favorite interview trap)

Our filter uses **pre-filtering**: restrict to matching items *first*, then rank (exact, always
returns k if enough matches exist). The alternative is **post-filtering**: run the ANN search,
then drop non-matching results (fast, but can return fewer than k, or miss good matches). Real
systems like Weaviate/Qdrant implement filtered-HNSW that blends both. Being able to name this
trade-off is a senior-level signal. See `VectorStore.search(...)`.

## Interview questions you can now answer confidently

1. *What is an embedding?* → doc 2.
2. *How does a vector DB find neighbors so fast?* → HNSW layered graph, ~O(log N), doc 6.
3. *Why not a KD-Tree for embeddings?* → curse of dimensionality, doc 5.
4. *Cosine vs euclidean — when?* → doc 3 (and the normalized-vector equivalence).
5. *What is RAG and why use it?* → grounding + fresh/private knowledge, fewer hallucinations,
   doc 7.
6. *RAG vs fine-tuning?* → knowledge vs behavior; cost; often both, doc 7.
7. *Your RAG answers are bad — what do you check?* → chunking, `k`, retrieval recall, doc 9/10.
8. *How do you measure/tune an ANN index?* → recall@k vs latency, tune `ef`, doc 10.
9. *How do you filter by metadata?* → pre/post-filter trade-off, above.
10. *How do you make it durable?* → persistence/snapshotting, `SnapshotRepository`.

## A good 30-second project pitch

> "I built a vector database and RAG system in Java from scratch — three nearest-neighbor
> algorithms (brute force, KD-Tree, HNSW), three distance metrics, metadata filtering, disk
> persistence, and a recall@k evaluation harness. It embeds documents with a local model via
> Ollama, retrieves the most relevant chunks with HNSW, and has an LLM answer using them. I can
> explain every trade-off: why HNSW beats KD-Trees in high dimensions, cosine vs euclidean, and
> how I measure retrieval quality against exact search."
