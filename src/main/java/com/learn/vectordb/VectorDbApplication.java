package com.learn.vectordb;

import com.learn.vectordb.config.VectorDbProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point for the educational Vector Database + RAG service.
 *
 * <p>The nearest-neighbor indexes (BruteForce, KD-Tree, HNSW) are hand-written from scratch
 * so the algorithms can be studied directly. See the {@code docs/} folder for a
 * plain-language explanation of every concept.
 */
@SpringBootApplication
@EnableConfigurationProperties(VectorDbProperties.class)
public class VectorDbApplication {

    public static void main(String[] args) {
        SpringApplication.run(VectorDbApplication.class, args);
    }
}
