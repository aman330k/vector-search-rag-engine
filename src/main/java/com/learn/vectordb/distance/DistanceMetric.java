package com.learn.vectordb.distance;

/**
 * The three distance metrics supported by the vector database.
 *
 * <p>A "distance" answers the question: how far apart are two vectors? Smaller means more
 * similar. Every search ranks candidates by ascending distance, so index 0 of a result is
 * always the closest match.
 */
public enum DistanceMetric {

    /**
     * Straight-line ("as the crow flies") distance. Sensitive to vector magnitude.
     * distance = sqrt(sum((a_i - b_i)^2))
     */
    EUCLIDEAN {
        @Override
        public float distance(float[] a, float[] b) {
            float sum = 0f;
            for (int i = 0; i < a.length; i++) {
                float d = a[i] - b[i];
                sum += d * d;
            }
            return (float) Math.sqrt(sum);
        }
    },

    /**
     * Cosine distance = 1 - cosine similarity. Measures the angle between vectors and ignores
     * magnitude, which is why it is the default for text embeddings. Range is [0, 2] where 0
     * means "pointing the same direction" (most similar).
     */
    COSINE {
        @Override
        public float distance(float[] a, float[] b) {
            float dot = 0f, normA = 0f, normB = 0f;
            for (int i = 0; i < a.length; i++) {
                dot += a[i] * b[i];
                normA += a[i] * a[i];
                normB += b[i] * b[i];
            }
            if (normA < 1e-9f || normB < 1e-9f) {
                return 1.0f;
            }
            return 1.0f - dot / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
        }
    },

    /**
     * Manhattan (taxicab / L1) distance: the sum of absolute per-dimension differences,
     * like walking a city grid. distance = sum(|a_i - b_i|)
     */
    MANHATTAN {
        @Override
        public float distance(float[] a, float[] b) {
            float sum = 0f;
            for (int i = 0; i < a.length; i++) {
                sum += Math.abs(a[i] - b[i]);
            }
            return sum;
        }
    };

    /**
     * Computes the distance between two equally-sized vectors.
     *
     * @param a first vector
     * @param b second vector (same length as {@code a})
     * @return distance where smaller means more similar
     */
    public abstract float distance(float[] a, float[] b);

    /**
     * Resolves a metric from its lowercase REST name (e.g. "cosine"), defaulting to
     * {@link #EUCLIDEAN} for anything unrecognized.
     *
     * @param name metric name, case-insensitive; may be null
     * @return the matching metric, or EUCLIDEAN when not recognized
     */
    public static DistanceMetric fromName(String name) {
        if (name == null) {
            return EUCLIDEAN;
        }
        switch (name.trim().toLowerCase()) {
            case "cosine":
                return COSINE;
            case "manhattan":
                return MANHATTAN;
            default:
                return EUCLIDEAN;
        }
    }

    /** @return the lowercase name used in the REST API and UI. */
    public String apiName() {
        return name().toLowerCase();
    }
}
