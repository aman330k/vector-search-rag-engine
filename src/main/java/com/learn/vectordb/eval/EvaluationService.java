package com.learn.vectordb.eval;

import com.learn.vectordb.model.SearchHit;
import com.learn.vectordb.model.SearchResponse;
import com.learn.vectordb.model.VectorItem;
import com.learn.vectordb.store.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Measures retrieval quality using <b>recall@k</b>.
 *
 * <p>Building an approximate index like HNSW is only half the job — you also have to prove it
 * returns the right answers. recall@k asks: of the true top-k nearest neighbors (found by the
 * exact brute-force search), how many did the approximate search also return? A recall of 1.0
 * means "found all of them"; 0.8 means "missed one in five". This is exactly how teams tune
 * ANN indexes in practice, and being able to talk about it is a strong mid/senior signal.
 */
@Service
public class EvaluationService {

    private final VectorStore vectorStore;

    public EvaluationService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Runs recall@k for KD-Tree and HNSW against brute-force ground truth.
     *
     * <p>Every stored vector is used as a query (a standard self-query evaluation), so the
     * result reflects the current demo data set.
     *
     * @param k      neighbors to compare
     * @param metric distance metric name
     * @return a report with recall and average latency per algorithm
     */
    public RecallReport evaluate(int k, String metric) {
        List<VectorItem> items = vectorStore.all();
        List<float[]> queries = new ArrayList<>();
        for (VectorItem item : items) {
            queries.add(item.embedding());
        }

        String[] algorithms = {"kdtree", "hnsw"};
        List<RecallReport.AlgorithmScore> scores = new ArrayList<>();

        for (String algo : algorithms) {
            double recallSum = 0.0;
            double latencySum = 0.0;
            for (float[] query : queries) {
                SearchResponse exact = vectorStore.search(query, k, metric, "bruteforce", null);
                SearchResponse approx = vectorStore.search(query, k, metric, algo, null);

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
                latencySum += approx.latencyUs();
            }
            int n = Math.max(queries.size(), 1);
            scores.add(new RecallReport.AlgorithmScore(algo, recallSum / n, latencySum / n));
        }

        return new RecallReport(k, metric, queries.size(), scores);
    }

    private Set<Integer> idSet(List<SearchHit> hits) {
        Set<Integer> ids = new HashSet<>();
        for (SearchHit hit : hits) {
            ids.add(hit.id());
        }
        return ids;
    }
}
