package com.learn.vectordb;

import com.learn.vectordb.store.VectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Smoke test: the whole Spring context wires up and the demo data loads at startup.
 * Persistence is disabled so the test does not touch the filesystem.
 */
@SpringBootTest
@TestPropertySource(properties = {"vectordb.persistence.enabled=false"})
class VectorDbApplicationTests {

    @Autowired
    private VectorStore vectorStore;

    @Test
    void contextLoadsAndDemoDataIsPresent() {
        assertEquals(20, vectorStore.size());
    }
}
