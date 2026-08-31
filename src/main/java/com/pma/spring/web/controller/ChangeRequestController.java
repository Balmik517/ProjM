package com.pma.spring.web.controller;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.web.entity.ChangeRequest;
import com.pma.spring.web.service.ChangeRequestService;

/**
 * Change request endpoints.
 *
 * Note that {@code LegacyAdminController} exposes a second, older API over the
 * same table through {@code LegacyWorkflowService}.
 */
@RestController
@RequestMapping("/change-requests")
public class ChangeRequestController {

    private static final Logger logger = Logger.getLogger(ChangeRequestController.class);

    @Autowired
    private ChangeRequestService changeRequestService;

    @GetMapping
    public ResponseEntity<List<ChangeRequest>> list(
            @RequestParam(value = "projectId", required = false) Integer projectId) {
        if (projectId != null) {
            return ResponseEntity.ok(changeRequestService.findForProject(projectId.intValue()));
        }
        return ResponseEntity.ok(changeRequestService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChangeRequest> get(@PathVariable("id") int changeRequestId) {
        try {
            return ResponseEntity.ok(changeRequestService.findById(changeRequestId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ChangeRequest> raise(@RequestBody Map<String, String> body) {
        try {
            int projectId = Integer.parseInt(body.get("projectId"));
            int requesterId = body.get("requesterId") == null ? 0 : Integer.parseInt(body.get("requesterId"));
            ChangeRequest request = changeRequestService.raise(projectId, body.get("title"),
                    body.get("description"), body.get("priority"), requesterId);
            return ResponseEntity.ok(request);
        } catch (RuntimeException e) {
            logger.warn("Change request creation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ChangeRequest> approve(@PathVariable("id") int changeRequestId,
            @RequestBody Map<String, String> body) {
        try {
            int approverId = body.get("approverId") == null ? 0 : Integer.parseInt(body.get("approverId"));
            return ResponseEntity.ok(changeRequestService.approveChangeRequest(changeRequestId, approverId,
                    body.get("note")));
        } catch (RuntimeException e) {
            logger.warn("Approval failed for change request " + changeRequestId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ChangeRequest> reject(@PathVariable("id") int changeRequestId,
            @RequestBody Map<String, String> body) {
        try {
            int approverId = body.get("approverId") == null ? 0 : Integer.parseInt(body.get("approverId"));
            return ResponseEntity.ok(changeRequestService.reject(changeRequestId, approverId, body.get("reason")));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/implement")
    public ResponseEntity<ChangeRequest> implement(@PathVariable("id") int changeRequestId,
            @RequestParam(value = "actorId", defaultValue = "0") int actorId) {
        try {
            return ResponseEntity.ok(changeRequestService.markImplemented(changeRequestId, actorId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/summary/{projectId}")
    public ResponseEntity<Map<String, Object>> summary(@PathVariable("projectId") int projectId) {
        return ResponseEntity.ok(changeRequestService.getChangeSummary(projectId));
    }
}
