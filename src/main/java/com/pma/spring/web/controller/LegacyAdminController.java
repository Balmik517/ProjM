package com.pma.spring.web.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.web.entity.ChangeRequest;
import com.pma.spring.web.service.LegacyWorkflowService;

/**
 * Legacy administrative controller exposing tightly coupled workflow operations.
 */
@RestController
@RequestMapping("/api/legacy")
public class LegacyAdminController {

    private static final Logger logger = Logger.getLogger(LegacyAdminController.class);

    @Autowired
    private LegacyWorkflowService legacyWorkflowService;

    @PostMapping("/change-request")
    public ResponseEntity<ChangeRequest> createChangeRequest(@RequestBody Map<String, String> body) {
        ChangeRequest request = legacyWorkflowService.createRequest(
                body.get("title"),
                body.get("description"),
                body.get("owner"),
                body.get("priority"));
        return ResponseEntity.ok(request);
    }

    @PostMapping("/change-request/{id}/transition")
    public ResponseEntity<ChangeRequest> transition(@PathVariable int id, @RequestBody Map<String, String> body) {
        String action = body.get("action");
        String actor = body.get("actor");
        ChangeRequest request = legacyWorkflowService.transition(id, action, actor);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/change-request/{id}/assign")
    public ResponseEntity<ChangeRequest> assign(@PathVariable int id, @RequestBody Map<String, String> body) {
        ChangeRequest request = legacyWorkflowService.assignOwner(id, body.get("owner"));
        return ResponseEntity.ok(request);
    }

    @PostMapping("/workflow/escalate")
    public ResponseEntity<Map<String, Object>> bulkEscalate(@RequestParam(defaultValue = "NEW") String status) {
        int count = legacyWorkflowService.bulkEscalate(status);
        Map<String, Object> response = new HashMap<>();
        response.put("escalated", count);
        response.put("status", status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/workflow/review/drain")
    public ResponseEntity<List<ChangeRequest>> drainReviewQueue(@RequestParam(defaultValue = "5") int maxItems) {
        return ResponseEntity.ok(legacyWorkflowService.drainManualReviewQueue(maxItems));
    }

    @GetMapping("/workflow/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(legacyWorkflowService.getDashboard());
    }

    @GetMapping("/workflow/events")
    public ResponseEntity<List<String>> events(@RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(legacyWorkflowService.getRecentEvents(size));
    }

    @GetMapping("/workflow/list")
    public ResponseEntity<List<ChangeRequest>> list() {
        List<ChangeRequest> list = legacyWorkflowService.findAll();
        logger.info("Returning workflow list with size " + (list == null ? 0 : list.size()));
        return ResponseEntity.ok(list);
    }
}
