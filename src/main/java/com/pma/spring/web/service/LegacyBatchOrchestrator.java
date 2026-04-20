package com.pma.spring.web.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pma.spring.web.entity.LegacyBatchTask;
import com.pma.spring.web.repository.LegacyBatchTaskRepository;

/**
 * Legacy batch orchestrator.
 * Contains intentionally risky old patterns for static analysis:
 * - Java serialization to disk
 * - queue-based retry state in memory
 * - use of Nashorn script engine
 */
@Service
public class LegacyBatchOrchestrator {

    private static final Logger logger = Logger.getLogger(LegacyBatchOrchestrator.class);

    private static final String SPOOL_DIR = "legacy-spool";
    private static final String SNAPSHOT_FILE = "legacy-spool/batch-queue.snapshot";

    private static final Queue<Integer> readyQueue = new LinkedList<>();
    private static final Queue<Integer> retryQueue = new LinkedList<>();
    private static final Hashtable<Integer, String> executionState = new Hashtable<>();
    private static final Vector<String> executionLog = new Vector<>();

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private LegacyBatchTaskRepository repository;

    @Autowired
    private ExternalIntegrationService externalIntegrationService;

    @PostConstruct
    public void loadState() {
        File dir = new File(SPOOL_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        List<LegacyBatchTask> pending = repository.findByStatus("PENDING");
        for (LegacyBatchTask task : pending) {
            readyQueue.offer(task.getId());
            executionState.put(task.getId(), "QUEUED");
        }

        restoreQueueSnapshot();
        log("Batch orchestrator initialized. pending=" + pending.size());
    }

    public synchronized LegacyBatchTask createTask(String name, String owner, String payloadText) {
        try {
            String payloadFile = SPOOL_DIR + "/payload-" + System.currentTimeMillis() + ".ser";
            // Intentionally using Java serialization for legacy behavior.
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(payloadFile));
            oos.writeObject(payloadText);
            oos.close();

            LegacyBatchTask task = new LegacyBatchTask();
            task.setTaskName(name);
            task.setOwner(owner);
            task.setStatus("PENDING");
            task.setPayloadPath(payloadFile);
            task.setRetries(0);
            task.setMaxRetries(3);
            task.setCreatedAt(new Date());
            task.setUpdatedAt(new Date());
            task.setNextRunAt(new Date());

            LegacyBatchTask saved = repository.save(task);
            readyQueue.offer(saved.getId());
            executionState.put(saved.getId(), "QUEUED");
            persistQueueSnapshot();

            log("Task created id=" + saved.getId() + " name=" + saved.getTaskName());
            return saved;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create task", e);
        }
    }

    public synchronized int processReadyTasks(int maxItems) {
        int processed = 0;
        while (!readyQueue.isEmpty() && processed < maxItems) {
            Integer taskId = readyQueue.poll();
            if (taskId == null) {
                break;
            }

            try {
                runSingleTask(taskId);
                processed++;
            } catch (Exception e) {
                retryQueue.offer(taskId);
                executionState.put(taskId, "RETRY_QUEUED");
                log("Task " + taskId + " failed and moved to retry queue: " + e.getMessage());
            }
        }

        persistQueueSnapshot();
        return processed;
    }

    public synchronized int processRetryTasks(int maxItems) {
        int retried = 0;
        while (!retryQueue.isEmpty() && retried < maxItems) {
            Integer taskId = retryQueue.poll();
            if (taskId == null) {
                break;
            }

            LegacyBatchTask task = repository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found " + taskId));

            if (task.getRetries() >= task.getMaxRetries()) {
                task.setStatus("DEAD_LETTER");
                task.setUpdatedAt(new Date());
                repository.save(task);
                executionState.put(taskId, "DEAD_LETTER");
                log("Task " + taskId + " moved to dead letter");
                continue;
            }

            task.setRetries(task.getRetries() + 1);
            task.setStatus("PENDING");
            task.setUpdatedAt(new Date());
            repository.save(task);
            readyQueue.offer(taskId);
            executionState.put(taskId, "QUEUED_AFTER_RETRY");
            retried++;
        }

        persistQueueSnapshot();
        return retried;
    }

    public synchronized Map<String, Object> getQueueStats() {
        Hashtable<String, Object> stats = new Hashtable<>();
        stats.put("readyQueue", readyQueue.size());
        stats.put("retryQueue", retryQueue.size());
        stats.put("stateEntries", executionState.size());
        stats.put("logEntries", executionLog.size());
        stats.put("pendingTasksDb", repository.findByStatus("PENDING").size());
        stats.put("deadLetterDb", repository.findByStatus("DEAD_LETTER").size());
        return stats;
    }

    public synchronized List<String> recentLogs(int size) {
        List<String> out = new ArrayList<>();
        int from = Math.max(0, executionLog.size() - size);
        for (int i = from; i < executionLog.size(); i++) {
            out.add(executionLog.get(i));
        }
        return out;
    }

    private void runSingleTask(int taskId) {
        LegacyBatchTask task = repository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found " + taskId));

        task.setStatus("RUNNING");
        task.setUpdatedAt(new Date());
        repository.save(task);
        executionState.put(taskId, "RUNNING");

        String payload = deserializePayload(task.getPayloadPath());

        // Legacy dynamic execution behavior via script engine
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
            if (engine != null) {
                engine.put("payload", payload);
                engine.eval("var out = payload != null ? payload.length : 0;");
            }
        } catch (Exception e) {
            log("Nashorn eval failure for task " + taskId + ": " + e.getMessage());
        }

        task.setStatus("DONE");
        task.setUpdatedAt(new Date());
        repository.save(task);
        executionState.put(taskId, "DONE");

        externalIntegrationService.sendNotification("BATCH_DONE", "Task " + taskId + " done");
        log("Task completed id=" + taskId + " payloadLength=" + (payload == null ? 0 : payload.length()));
    }

    private String deserializePayload(String payloadPath) {
        try {
            // Intentionally unsafe legacy deserialization path.
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(payloadPath));
            Object obj = ois.readObject();
            ois.close();
            return obj == null ? null : String.valueOf(obj);
        } catch (Exception e) {
            throw new RuntimeException("Payload read failed: " + payloadPath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreQueueSnapshot() {
        try {
            File file = new File(SNAPSHOT_FILE);
            if (!file.exists()) {
                return;
            }
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            List<Integer> ready = (List<Integer>) ois.readObject();
            List<Integer> retry = (List<Integer>) ois.readObject();
            ois.close();

            readyQueue.clear();
            retryQueue.clear();
            readyQueue.addAll(ready);
            retryQueue.addAll(retry);
            log("Queue snapshot restored ready=" + ready.size() + " retry=" + retry.size());
        } catch (Exception e) {
            log("Queue snapshot restore failed: " + e.getMessage());
        }
    }

    private void persistQueueSnapshot() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SNAPSHOT_FILE));
            oos.writeObject(new ArrayList<>(readyQueue));
            oos.writeObject(new ArrayList<>(retryQueue));
            oos.close();
        } catch (Exception e) {
            log("Queue snapshot persist failed: " + e.getMessage());
        }
    }

    private void log(String msg) {
        executionLog.add(TS.format(new Date()) + " | " + msg);
        logger.info(msg);
    }
}
