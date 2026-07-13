package com.learn.vectordb.web;

import com.learn.vectordb.store.SnapshotRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin endpoints for the disk-persistence feature.
 *
 * <ul>
 *   <li>{@code POST /admin/save} — write the current document store to disk</li>
 *   <li>{@code POST /admin/load} — reload the document store from disk</li>
 * </ul>
 */
@RestController
public class AdminController {

    private final SnapshotRepository snapshotRepository;

    public AdminController(SnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @PostMapping("/admin/save")
    public Map<String, Object> save() {
        int saved = snapshotRepository.save();
        return Map.of("saved", saved);
    }

    @PostMapping("/admin/load")
    public Map<String, Object> load() {
        int loaded = snapshotRepository.load();
        return Map.of("loaded", loaded);
    }
}
