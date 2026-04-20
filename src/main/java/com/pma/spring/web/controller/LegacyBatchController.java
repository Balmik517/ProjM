package com.pma.spring.web.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.web.entity.LegacyBatchTask;
import com.pma.spring.web.service.LegacyBatchOrchestrator;

@RestController
@RequestMapping("/api/legacy/batch")
public class LegacyBatchController {

    @Autowired
    private LegacyBatchOrchestrator legacyBatchOrchestrator;

    @PostMapping("/task")
    public ResponseEntity<LegacyBatchTask> create(@RequestBody Map<String, String> body) {
        LegacyBatchTask task = legacyBatchOrchestrator.createTask(
                body.get("taskName"),
                body.get("owner"),
                body.get("payload"));
        return ResponseEntity.ok(task);
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> process(@RequestParam(defaultValue = "5") int maxItems) {
        int processed = legacyBatchOrchestrator.processReadyTasks(maxItems);
        int retried = legacyBatchOrchestrator.processRetryTasks(maxItems);
        Map<String, Object> result = new HashMap<>();
        result.put("processed", processed);
        result.put("retried", retried);
        result.put("stats", legacyBatchOrchestrator.getQueueStats());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(legacyBatchOrchestrator.getQueueStats());
    }

    @GetMapping("/logs")
    public ResponseEntity<List<String>> logs(@RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(legacyBatchOrchestrator.recentLogs(size));
    }
}
