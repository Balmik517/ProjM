package com.pma.spring.web.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.web.dto.InvoiceRequest;
import com.pma.spring.web.dto.NotificationRequest;
import com.pma.spring.web.dto.PaymentRequest;
import com.pma.spring.web.dto.ProjectCreateRequest;
import com.pma.spring.web.dto.ProjectMemberRequest;
import com.pma.spring.web.dto.ProjectTaskRequest;
import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.Notification;
import com.pma.spring.web.entity.Payment;
import com.pma.spring.web.entity.ProjectMember;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.entity.ReportSnapshot;
import com.pma.spring.web.service.ProjectManagementService;

/**
 * Operations API sitting on top of the wide project-management facade.
 *
 * One controller, one downstream bean, and behind it every domain in the
 * application - a controller-to-service edge that carries far more weight than
 * it appears to.
 */
@RestController
@RequestMapping("/operations")
public class ProjectOperationsController {

    private static final Logger logger = Logger.getLogger(ProjectOperationsController.class);

    @Autowired
    private ProjectManagementService projectManagementService;

    @PostMapping("/projects")
    public ResponseEntity<ProjectRegister> createProject(@RequestBody ProjectCreateRequest request) {
        try {
            ProjectRegister project = projectManagementService.createProject(request.getProjectName(),
                    request.getStartDate(), request.getEndDate(), request.getOwnerUserId());
            return ResponseEntity.ok(project);
        } catch (RuntimeException e) {
            logger.warn("Project creation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<ProjectRegister> updateProject(@PathVariable("id") int projectId,
            @RequestBody ProjectCreateRequest request,
            @RequestParam(value = "actorId", defaultValue = "0") int actorId) {
        try {
            return ResponseEntity.ok(projectManagementService.updateProject(projectId, request.getProjectName(),
                    request.getEndDate(), actorId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/projects/{id}/members")
    public ResponseEntity<ProjectMember> assignMember(@PathVariable("id") int projectId,
            @RequestBody ProjectMemberRequest request) {
        try {
            return ResponseEntity.ok(projectManagementService.assignMember(projectId, request.getUserId(),
                    request.getRole(), request.getActorId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/projects/{id}/tasks")
    public ResponseEntity<ProjectTask> createTask(@PathVariable("id") int projectId,
            @RequestBody ProjectTaskRequest request) {
        try {
            return ResponseEntity.ok(projectManagementService.createTask(projectId, request.getAssignedTo(),
                    request.getTitle(), request.getPriority(), request.getEstimatedHours(), request.getActorId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/projects/{id}/invoices")
    public ResponseEntity<Invoice> generateInvoice(@PathVariable("id") int projectId,
            @RequestBody InvoiceRequest request) {
        try {
            return ResponseEntity.ok(projectManagementService.generateInvoice(projectId, request.getCustomerId(),
                    request.getAmount(), request.getActorId()));
        } catch (RuntimeException e) {
            logger.warn("Invoice generation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/payments")
    public ResponseEntity<Payment> processPayment(@RequestBody PaymentRequest request) {
        try {
            return ResponseEntity.ok(projectManagementService.processPayment(request.getInvoiceId(),
                    request.getAmount(), request.getActorId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/notifications")
    public ResponseEntity<Notification> sendNotification(@RequestBody NotificationRequest request) {
        return ResponseEntity.ok(projectManagementService.sendNotification(request.getUserId(),
                request.getProjectId(), request.getType(), request.getMessage()));
    }

    @PostMapping("/projects/{id}/reports")
    public ResponseEntity<ReportSnapshot> generateReport(@PathVariable("id") int projectId,
            @RequestParam(value = "generatedBy", defaultValue = "operations") String generatedBy,
            @RequestParam(value = "actorId", defaultValue = "0") int actorId) {
        try {
            return ResponseEntity.ok(projectManagementService.generateReport(projectId, generatedBy, actorId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/projects/{id}/reprioritise")
    public ResponseEntity<Integer> reprioritise(@PathVariable("id") int projectId,
            @RequestParam(value = "priority", defaultValue = "HIGH") String priority,
            @RequestParam(value = "actorId", defaultValue = "0") int actorId) {
        return ResponseEntity.ok(Integer.valueOf(
                projectManagementService.reprioritiseTasks(projectId, priority, actorId)));
    }

    /**
     * The wide completion workflow - one call, eight domains, one transaction.
     */
    @PostMapping("/projects/{id}/complete")
    public ResponseEntity<Map<String, Object>> completeProject(@PathVariable("id") int projectId,
            @RequestParam(value = "actorId", defaultValue = "0") int actorId) {
        try {
            return ResponseEntity.ok(projectManagementService.completeProject(projectId, actorId));
        } catch (RuntimeException e) {
            logger.error("Project completion failed for " + projectId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/projects/{id}/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(@PathVariable("id") int projectId) {
        try {
            return ResponseEntity.ok(projectManagementService.dashboard(projectId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/billing/overview")
    public ResponseEntity<Map<String, Object>> billingOverview() {
        return ResponseEntity.ok(projectManagementService.billingOverview());
    }

    @GetMapping("/projects/{id}/notifications")
    public ResponseEntity<List<Notification>> listNotifications(@PathVariable("id") int projectId) {
        return ResponseEntity.ok(projectManagementService.listNotifications(projectId));
    }

    @GetMapping(value = "/reports/legacy-html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> legacyHtmlReport() {
        return ResponseEntity.ok(projectManagementService.generateLegacyHtmlReport());
    }

    @PostMapping("/projects/{id}/integrations")
    public ResponseEntity<Map<String, Object>> queueIntegration(@PathVariable("id") int projectId,
            @RequestParam(value = "type", defaultValue = "PROJECT_SYNC") String type,
            @RequestParam(value = "payload", defaultValue = "") String payload) {
        java.util.Map<String, Object> body = new java.util.HashMap<String, Object>();
        body.put("requestId",
                Integer.valueOf(projectManagementService.queueIntegration(projectId, type, payload).getId()));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/billing/summary")
    public ResponseEntity<BigDecimal> billingSummaryPlaceholder() {
        Map<String, Object> overview = projectManagementService.billingOverview();
        Object total = overview.get("totalInvoices");
        return ResponseEntity.ok(BigDecimal.valueOf(total == null ? 0L : ((Number) total).longValue()));
    }
}
