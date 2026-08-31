package com.pma.spring.web.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.web.dto.IntegrationRequestDto;
import com.pma.spring.web.entity.IntegrationRequest;
import com.pma.spring.web.service.IntegrationService;

/**
 * Integration outbox endpoints. Like the notification controller this one has a
 * single downstream component.
 */
@RestController
@RequestMapping("/integrations")
public class IntegrationController {

    @Autowired
    private IntegrationService integrationService;

    @GetMapping
    public ResponseEntity<List<IntegrationRequest>> listRequests(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "projectId", required = false) Integer projectId) {

        if (status != null) {
            return ResponseEntity.ok(integrationService.findByStatus(status));
        }
        if (projectId != null) {
            return ResponseEntity.ok(integrationService.findForProject(projectId.intValue()));
        }
        return ResponseEntity.ok(integrationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IntegrationRequest> getRequest(@PathVariable("id") int requestId) {
        try {
            return ResponseEntity.ok(integrationService.findById(requestId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<IntegrationRequest> enqueue(@RequestBody IntegrationRequestDto request) {
        return ResponseEntity.ok(integrationService.enqueue(request.getProjectId(), request.getIntegrationType(),
                request.getPayload()));
    }

    @PostMapping("/{id}/dispatch")
    public ResponseEntity<IntegrationRequest> dispatch(@PathVariable("id") int requestId) {
        try {
            return ResponseEntity.ok(integrationService.dispatch(requestId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<IntegrationRequest> retry(@PathVariable("id") int requestId) {
        try {
            return ResponseEntity.ok(integrationService.retry(requestId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(integrationService.getQueueStats());
    }
}
