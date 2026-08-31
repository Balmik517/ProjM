package com.pma.spring.web.controller;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.web.entity.ReportSnapshot;
import com.pma.spring.web.service.ReportingService;

/**
 * Reporting endpoints.
 *
 * A thin controller over a component that reads most of the database, so the
 * shallow controller-to-service edge understates how much this surface pulls
 * in.
 */
@RestController
@RequestMapping("/reports")
public class ReportingController {

    private static final Logger logger = Logger.getLogger(ReportingController.class);

    @Autowired
    private ReportingService reportingService;

    @GetMapping("/projects/{id}")
    public ResponseEntity<Map<String, Object>> projectReport(@PathVariable("id") int projectId,
            @RequestParam(value = "generatedBy", defaultValue = "api") String generatedBy,
            @RequestParam(value = "actorId", defaultValue = "0") int actorId) {
        try {
            ReportSnapshot snapshot = reportingService.generateProjectReport(projectId, generatedBy, actorId);
            java.util.Map<String, Object> body = new java.util.HashMap<String, Object>();
            body.put("snapshotId", Integer.valueOf(snapshot.getId()));
            body.put("projectId", Integer.valueOf(snapshot.getProjectId()));
            body.put("reportType", snapshot.getReportType());
            body.put("generatedBy", snapshot.getGeneratedBy());
            body.put("data", snapshot.getSnapshotData());
            return ResponseEntity.ok(body);
        } catch (RuntimeException e) {
            logger.warn("Project report failed for " + projectId + ": " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/projects/{id}/snapshots")
    public ResponseEntity<List<ReportSnapshot>> snapshots(@PathVariable("id") int projectId) {
        return ResponseEntity.ok(reportingService.findSnapshots(projectId));
    }

    @GetMapping("/snapshots")
    public ResponseEntity<List<ReportSnapshot>> allSnapshots() {
        return ResponseEntity.ok(reportingService.findAllSnapshots());
    }

    @GetMapping("/billing")
    public ResponseEntity<Map<String, Object>> billingReport() {
        return ResponseEntity.ok(reportingService.generateBillingReport());
    }

    @GetMapping("/operational")
    public ResponseEntity<Map<String, Object>> operationalSummary() {
        return ResponseEntity.ok(reportingService.generateOperationalSummary());
    }

    @GetMapping("/workload")
    public ResponseEntity<List<Map<String, Object>>> workloadReport() {
        return ResponseEntity.ok(reportingService.generateWorkloadReport());
    }

    @GetMapping("/changes")
    public ResponseEntity<Map<String, Object>> changeReport() {
        return ResponseEntity.ok(reportingService.generateChangeReport());
    }

    @GetMapping(value = "/legacy/users.csv", produces = "text/csv")
    public ResponseEntity<String> legacyUserCsv() {
        return ResponseEntity.ok(reportingService.exportLegacyUserCsv());
    }

    @PostMapping("/projects/{id}/billing-snapshot")
    public ResponseEntity<ReportSnapshot> captureBillingSnapshot(@PathVariable("id") int projectId,
            @RequestParam(value = "actorId", defaultValue = "0") int actorId) {
        try {
            return ResponseEntity.ok(reportingService.captureBillingSnapshot(projectId, "MANUAL", actorId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/ping", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("reporting-ok");
    }
}
