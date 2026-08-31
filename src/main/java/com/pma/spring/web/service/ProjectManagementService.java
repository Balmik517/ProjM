package com.pma.spring.web.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.web.entity.ChangeRequest;
import com.pma.spring.web.entity.IntegrationRequest;
import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.Notification;
import com.pma.spring.web.entity.Payment;
import com.pma.spring.web.entity.ProjectMember;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.entity.ReportSnapshot;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.ProjectMemberRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.util.LegacyUtils;
import com.pma.spring.web.util.ServiceLocator;

/**
 * Single entry point the legacy screens and batch jobs were pointed at.
 *
 * Over the years it accumulated one method per feature request, so it now
 * covers identity, project, membership, task, change, billing, payment,
 * notification, reporting and integration concerns in one class. It also
 * writes {@code project_register} and {@code project_tasks} directly instead of
 * going through the components that own them, and looks one collaborator up
 * through the static service locator instead of injecting it.
 *
 * Nothing else depends on this class, so its fan-out is entirely outbound.
 */
@Service
public class ProjectManagementService {

    private static final Logger logger = Logger.getLogger(ProjectManagementService.class);

    // ---- every domain component ----

    @Autowired
    private ProjectDomainService projectDomainService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ChangeRequestService changeRequestService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ReportingService reportingService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommonService commonService;

    // ---- and a set of repositories it reaches for directly ----

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    // ---------------------------------------------------------------
    // Identity
    // ---------------------------------------------------------------

    public UserRegister registerUser(UserRegister user) {
        UserRegister saved = userService.save(user);
        auditService.record(AuditService.ENTITY_USER, saved.getId(), "USER_REGISTERED", saved.getId(),
                "email=" + StringUtils.defaultString(saved.getEmail()));
        LegacyUtils.recordDomainTouch("god", "registerUser");
        return saved;
    }

    public List<UserRegister> listUsers() {
        List<UserRegister> users = userService.findAll();
        return users == null ? new ArrayList<UserRegister>() : users;
    }

    // ---------------------------------------------------------------
    // Projects
    // ---------------------------------------------------------------

    public ProjectRegister createProject(String name, Date startDate, Date endDate, int ownerUserId) {
        LegacyUtils.recordDomainTouch("god", "createProject");
        return projectDomainService.createProject(name, startDate, endDate, ownerUserId);
    }

    /**
     * Writes the project row directly rather than going through the project
     * component, making this the third writer of that table.
     */
    @Transactional
    public ProjectRegister updateProject(int projectId, String name, Date endDate, int actorId) {
        Optional<ProjectRegister> optional = projectRepository.findById(Integer.valueOf(projectId));
        if (!optional.isPresent()) {
            throw new RuntimeException("Project not found for id " + projectId);
        }

        ProjectRegister project = optional.get();
        if (StringUtils.isNotBlank(name)) {
            project.setProjectName(name);
        }
        if (endDate != null) {
            project.setEndDate(endDate);
        }
        ProjectRegister saved = projectRepository.save(project);

        auditService.record(AuditService.ENTITY_PROJECT, projectId, "PROJECT_UPDATED", actorId,
                "name=" + saved.getProjectName());
        LegacyUtils.recordDomainTouch("god", "updateProject");
        return saved;
    }

    public ProjectMember assignMember(int projectId, int userId, String role, int actorId) {
        LegacyUtils.recordDomainTouch("god", "assignMember");
        return projectDomainService.assignMember(projectId, userId, role, actorId);
    }

    public List<ProjectMember> listMembers(int projectId) {
        return projectMemberRepository.findByProjectId(projectId);
    }

    // ---------------------------------------------------------------
    // Tasks
    // ---------------------------------------------------------------

    public ProjectTask createTask(int projectId, int assignedTo, String title, String priority,
            double estimatedHours, int actorId) {
        LegacyUtils.recordDomainTouch("god", "createTask");
        return taskService.createTask(projectId, assignedTo, title, priority, estimatedHours, actorId);
    }

    /**
     * Bulk reprioritise implemented against the task repository directly.
     */
    @Transactional
    public int reprioritiseTasks(int projectId, String priority, int actorId) {
        List<ProjectTask> tasks = projectTaskRepository.findByProjectId(projectId);
        String normalized = LegacyUtils.normalizeStatus(priority, "MEDIUM");
        for (ProjectTask task : tasks) {
            task.setPriority(normalized);
            task.setUpdatedAt(new Date());
            projectTaskRepository.save(task);
        }
        auditService.record(AuditService.ENTITY_PROJECT, projectId, "TASKS_REPRIORITISED", actorId,
                "count=" + tasks.size() + ";priority=" + normalized);
        return tasks.size();
    }

    // ---------------------------------------------------------------
    // Change requests
    // ---------------------------------------------------------------

    public ChangeRequest raiseChange(int projectId, String title, String description, String priority,
            int requesterId) {
        LegacyUtils.recordDomainTouch("god", "raiseChange");
        return changeRequestService.raise(projectId, title, description, priority, requesterId);
    }

    public ChangeRequest approveChange(int changeRequestId, int approverId, String note) {
        LegacyUtils.recordDomainTouch("god", "approveChange");
        return changeRequestService.approveChangeRequest(changeRequestId, approverId, note);
    }

    // ---------------------------------------------------------------
    // Billing and payments
    // ---------------------------------------------------------------

    public Invoice generateInvoice(int projectId, int customerId, BigDecimal amount, int actorId) {
        LegacyUtils.recordDomainTouch("god", "generateInvoice");
        return billingService.createInvoice(projectId, customerId, amount, actorId);
    }

    public Payment processPayment(int invoiceId, BigDecimal amount, int actorId) {
        LegacyUtils.recordDomainTouch("god", "processPayment");
        return billingService.settleInvoice(invoiceId, amount, actorId);
    }

    public Map<String, Object> billingOverview() {
        return billingService.getBillingOverview();
    }

    // ---------------------------------------------------------------
    // Notifications
    // ---------------------------------------------------------------

    public Notification sendNotification(int userId, int projectId, String type, String message) {
        LegacyUtils.recordDomainTouch("god", "sendNotification");
        return notificationService.queue(userId, projectId, type, message);
    }

    public List<Notification> listNotifications(int projectId) {
        return notificationRepository.findByProjectId(projectId);
    }

    // ---------------------------------------------------------------
    // Reporting
    // ---------------------------------------------------------------

    public ReportSnapshot generateReport(int projectId, String generatedBy, int actorId) {
        LegacyUtils.recordDomainTouch("god", "generateReport");
        return reportingService.generateProjectReport(projectId, generatedBy, actorId);
    }

    /**
     * Resolves the report writer through the static service locator instead of
     * injection - a second, invisible edge in the dependency graph.
     */
    public String generateLegacyHtmlReport() {
        ReportService located = ServiceLocator.getService("reportService");
        if (located == null) {
            return "<html><body>Report service unavailable</body></html>";
        }
        return located.generateHtmlReport();
    }

    // ---------------------------------------------------------------
    // Integration
    // ---------------------------------------------------------------

    public IntegrationRequest queueIntegration(int projectId, String type, String payload) {
        LegacyUtils.recordDomainTouch("god", "queueIntegration");
        return integrationService.enqueue(projectId, type, payload);
    }

    // ---------------------------------------------------------------
    // The wide workflow
    // ---------------------------------------------------------------

    /**
     * End-to-end project completion.
     *
     * One transaction spans project, task, invoice, payment, notification,
     * audit, reporting and integration writes. Every one of those would become
     * a separate service call - and a separate commit - after decomposition,
     * which is exactly the risk this workflow is here to expose.
     */
    @Transactional
    public Map<String, Object> completeProject(int projectId, int actorId) {
        logger.info("Completing project " + projectId);

        // 1. project row
        Optional<ProjectRegister> optional = projectRepository.findById(Integer.valueOf(projectId));
        if (!optional.isPresent()) {
            throw new RuntimeException("Project not found for id " + projectId);
        }
        ProjectRegister project = optional.get();
        project.setStatus(ProjectDomainService.PROJECT_COMPLETED);
        project.setCompletedAt(new Date());
        projectRepository.save(project);

        // 2. close outstanding tasks
        int closedTasks = taskService.closeAllForProject(projectId, actorId);

        // 3. final invoice priced from the task table
        BigDecimal finalAmount = billingService.generateFinalInvoiceForProject(projectId, actorId);

        // 4. settle whatever is still open on this project
        int settledInvoices = 0;
        for (Invoice invoice : invoiceRepository.findByProjectId(projectId)) {
            if (BillingService.INVOICE_SETTLED.equals(invoice.getStatus())
                    || BillingService.INVOICE_CANCELLED.equals(invoice.getStatus())) {
                continue;
            }
            BigDecimal outstanding = LegacyUtils.safeAmount(invoice.getAmount())
                    .subtract(paymentService.settledTotalForInvoice(invoice.getId()));
            if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
                paymentService.recordPayment(invoice.getId(), outstanding, actorId);
                settledInvoices++;
            }
        }

        // 5. tell the owner
        notificationService.queueForProjectOwner(projectId, NotificationService.TYPE_PROJECT_COMPLETED,
                "Project " + project.getProjectName() + " completed");

        // 6. audit trail
        Map<String, Object> auditPayload = new HashMap<String, Object>();
        auditPayload.put("closedTasks", Integer.valueOf(closedTasks));
        auditPayload.put("finalAmount", finalAmount);
        auditPayload.put("settledInvoices", Integer.valueOf(settledInvoices));
        auditService.record(AuditService.ENTITY_PROJECT, projectId, "PROJECT_COMPLETED", actorId, auditPayload);

        // 7. final report snapshot
        ReportSnapshot snapshot = reportingService.generateProjectReport(projectId, "completion-workflow", actorId);

        // 8. outbound sync
        IntegrationRequest integration = integrationService.enqueue(projectId, IntegrationService.TYPE_PROJECT_SYNC,
                "projectId=" + projectId + ";status=" + project.getStatus() + ";finalAmount=" + finalAmount);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("projectId", Integer.valueOf(projectId));
        result.put("projectStatus", project.getStatus());
        result.put("closedTasks", Integer.valueOf(closedTasks));
        result.put("finalInvoiceAmount", finalAmount);
        result.put("settledInvoices", Integer.valueOf(settledInvoices));
        result.put("reportSnapshotId", Integer.valueOf(snapshot.getId()));
        result.put("integrationRequestId", Integer.valueOf(integration.getId()));

        LegacyUtils.recordDomainTouch("god", "completeProject");
        logger.info("Project " + projectId + " completed: " + result);
        return result;
    }

    /**
     * Console-style dashboard that pulls a little from everywhere.
     */
    public Map<String, Object> dashboard(int projectId) {
        Map<String, Object> dashboard = new HashMap<String, Object>();
        dashboard.put("project", projectDomainService.getProjectStatusSummary(projectId));
        dashboard.put("effort", taskService.getEffortSummary(projectId));
        dashboard.put("changes", changeRequestService.getChangeSummary(projectId));
        dashboard.put("billing", billingService.getProjectBillingView(projectId));
        dashboard.put("payments", paymentService.getPaymentStats());
        dashboard.put("notifications", notificationService.getQueueStats());
        dashboard.put("audit", auditService.getAuditSummary());
        dashboard.put("integrations", integrationService.getQueueStats());
        dashboard.put("environment", commonService.describeEnvironment());
        dashboard.put("knownUsers", Integer.valueOf(userRepository.findAll().size()));
        return dashboard;
    }
}
