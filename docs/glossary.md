# Glossary

Every term used in this project, in one line each.

- **ANN (Approximate Nearest Neighbor):** search that finds *almost* the closest vectors, trading
  a little accuracy for a lot of speed. HNSW is an ANN algorithm.
- **Brute force / exact search:** compare the query to every stored vector. Always correct, slow
  at scale. Our ground truth.
- **Chunk:** a small piece of a document that gets its own embedding.
- **Chunking:** the process of splitting text into chunks (with overlap).
- **Context window:** the maximum amount of text an LLM can read at once.
- **Cosine distance:** 1 − cosine similarity; measures angle (direction), ignores magnitude.
  Default for text.
- **Curse of dimensionality:** in high dimensions everything looks equally far apart, which
  breaks KD-Tree pruning.
- **Dimension:** one number (one slot) in a vector. Our demo vectors have 16; embeddings have 768.
- **Distance metric:** the rule for measuring closeness (cosine, euclidean, manhattan).
- **ef / efConstruction:** HNSW search width at query time (`ef`) and build time
  (`efConstruction`). Higher = more accurate, slower.
- **Embedding:** a vector that represents the *meaning* of text (or image, etc.).
- **Embedding model:** the model that produces embeddings (here: `nomic-embed-text`).
- **Euclidean distance (L2):** straight-line distance; sensitive to magnitude.
- **Generation model / LLM:** the model that writes text answers (here: `llama3.2`).
- **Ground truth:** the known-correct answer used to score approximate methods (brute force here).
- **Hallucination:** when an LLM confidently states something false. RAG reduces this.
- **HNSW (Hierarchical Navigable Small World):** multi-layer graph index; fast, approximate,
  high-dimensional. The industry standard.
- **k / top-k:** how many nearest neighbors you ask for.
- **KD-Tree:** binary space-partitioning tree; exact and fast in low dimensions only.
- **k-NN (k-Nearest Neighbors):** the k closest vectors to a query.
- **Latency:** how long a query takes (we report microseconds, μs).
- **Manhattan distance (L1):** sum of absolute differences; "city grid" distance.
- **Metadata filtering:** narrowing search by labels (e.g. category) before/after ranking.
- **Ollama:** app that runs LLMs locally and exposes an HTTP API on port 11434.
- **Overlap:** words shared between consecutive chunks so meaning isn't cut at the seam.
- **Persistence / snapshot:** saving data to disk so it survives restarts.
- **Post-filter / pre-filter:** filter after vs before the vector search (trade-offs in doc 11).
- **PCA (Principal Component Analysis):** math trick to squash high-D vectors to 2D for the
  scatter plot.
- **precision@k:** of the results you returned, the fraction that were relevant.
- **RAG (Retrieval-Augmented Generation):** retrieve relevant chunks, then have the LLM generate
  an answer using them.
- **recall@k:** of the true top-k, the fraction your search actually found. Key quality metric.
- **Semantic search:** searching by meaning (vectors) rather than exact keywords.
- **Vector:** an ordered list of numbers; a point in space.
- **Vector database:** a system that stores vectors and answers nearest-neighbor queries.
