package com.pma.spring.web.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.web.entity.ChangeRequest;
import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.entity.ReportSnapshot;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.AuditEventRepository;
import com.pma.spring.web.repository.ChangeRequestRepository;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.PaymentRepository;
import com.pma.spring.web.repository.ProjectMemberRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.repository.ReportSnapshotRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.util.DatabaseHelper;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Cross-domain reporting.
 *
 * Technically separable - it owns {@code report_snapshots} and is almost purely
 * read-only elsewhere - but it reads nine tables spanning every conceptual
 * domain, calls back into the project and change-request components, and takes
 * a second route to the same data through the shared JDBC helper. Extracting it
 * as-is would replace one query with a fan-out of remote calls.
 */
@Service
public class ReportingService {

    private static final Logger logger = Logger.getLogger(ReportingService.class);

    public static final String REPORT_PROJECT = "PROJECT_REPORT";
    public static final String REPORT_BILLING = "BILLING_REPORT";
    public static final String REPORT_OPERATIONAL = "OPERATIONAL_SUMMARY";

    // ---- repositories reaching across every domain ----

    @Autowired
    private ReportSnapshotRepository reportSnapshotRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChangeRequestRepository changeRequestRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    // ---- outbound service calls ----

    /** Cycle edge back into the project component. */
    @Autowired
    private ProjectDomainService projectDomainService;

    /** Cycle edge back into the change-request component. */
    @Autowired
    private ChangeRequestService changeRequestService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommonService commonService;

    @Autowired
    private NotificationService notificationService;

    /** The original legacy report writer, reused for the file based outputs. */
    @Autowired
    private ReportService reportService;

    /** Third route to the same tables, outside JPA. */
    @Autowired
    private DatabaseHelper databaseHelper;

    /**
     * Full project report. Pulls project, membership, task, change-request,
     * invoice, payment and identity data, then persists a snapshot and tells
     * the requester it is ready.
     */
    @Transactional
    public ReportSnapshot generateProjectReport(int projectId, String generatedBy, int actorId) {
        ProjectRegister project = loadProject(projectId);

        Map<String, Object> statusSummary = projectDomainService.getProjectStatusSummary(projectId);
        Map<String, Object> changeSummary = changeRequestService.getChangeSummary(projectId);

        List<ProjectTask> tasks = projectTaskRepository.findByProjectId(projectId);
        List<Invoice> invoices = invoiceRepository.findByProjectId(projectId);

        BigDecimal invoiced = LegacyUtils.safeAmount(invoiceRepository.sumAmountByProject(projectId));
        BigDecimal settled = BigDecimal.ZERO;
        for (Invoice invoice : invoices) {
            settled = settled.add(LegacyUtils.safeAmount(paymentRepository.sumSettledByInvoice(invoice.getId())));
        }

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("projectName", project.getProjectName());
        values.put("projectStatus", statusSummary.get("status"));
        values.put("members", statusSummary.get("activeMembers"));
        values.put("tasks", Integer.valueOf(tasks.size()));
        values.put("openTasks", statusSummary.get("openTasks"));
        values.put("actualHours", statusSummary.get("actualHours"));
        values.put("changeRequests", changeSummary.get("total"));
        values.put("changesApproved", changeSummary.get("approved"));
        values.put("invoices", Integer.valueOf(invoices.size()));
        values.put("invoiced", LegacyUtils.safeAmount(invoiced));
        values.put("settled", LegacyUtils.safeAmount(settled));
        values.put("outstanding", LegacyUtils.safeAmount(invoiced.subtract(settled)));
        values.put("notifications", Integer.valueOf(notificationRepository.findByProjectId(projectId).size()));
        values.put("auditEvents",
                Integer.valueOf(auditEventRepository.findByEntityTypeAndEntityId(
                        AuditService.ENTITY_PROJECT, projectId).size()));

        ReportSnapshot snapshot = persistSnapshot(projectId, REPORT_PROJECT, generatedBy, values);

        notificationService.queueForProjectOwner(projectId, NotificationService.TYPE_REPORT_READY,
                "Project report " + snapshot.getId() + " is ready");

        LegacyUtils.recordDomainTouch("reporting", "generateProjectReport");
        logger.info("Project report generated for project " + projectId);
        return snapshot;
    }

    /**
     * Billing-wide report that reads the billing tables directly rather than
     * asking the billing component, hiding the dependency from a service-call
     * graph while keeping it very much present in the data graph.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateBillingReport() {
        List<Invoice> invoices = invoiceRepository.findAll();

        BigDecimal invoiced = BigDecimal.ZERO;
        BigDecimal settled = BigDecimal.ZERO;
        Map<String, Integer> byStatus = new HashMap<String, Integer>();

        for (Invoice invoice : invoices) {
            invoiced = invoiced.add(LegacyUtils.safeAmount(invoice.getAmount()));
            settled = settled.add(LegacyUtils.safeAmount(paymentRepository.sumSettledByInvoice(invoice.getId())));

            String status = invoice.getStatus() == null ? "UNKNOWN" : invoice.getStatus();
            Integer count = byStatus.get(status);
            byStatus.put(status, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        }

        Map<String, Object> report = new HashMap<String, Object>();
        report.put("invoiceCount", Integer.valueOf(invoices.size()));
        report.put("paymentCount", Long.valueOf(paymentRepository.count()));
        report.put("totalInvoiced", LegacyUtils.safeAmount(invoiced));
        report.put("totalSettled", LegacyUtils.safeAmount(settled));
        report.put("outstanding", LegacyUtils.safeAmount(invoiced.subtract(settled)));
        report.put("byStatus", byStatus);
        report.put("customers", Long.valueOf(userRepository.count()));

        LegacyUtils.recordDomainTouch("reporting", "generateBillingReport");
        return report;
    }

    /**
     * Whole-system operational summary. Every conceptual domain contributes,
     * and part of it is read through raw SQL.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateOperationalSummary() {
        Map<String, Object> summary = new HashMap<String, Object>();

        summary.put("users", Long.valueOf(userRepository.count()));
        summary.put("projects", Long.valueOf(projectRepository.count()));
        summary.put("projectMembers", Long.valueOf(projectMemberRepository.count()));
        summary.put("tasks", Long.valueOf(projectTaskRepository.count()));
        summary.put("changeRequests", Long.valueOf(changeRequestRepository.count()));
        summary.put("invoices", Long.valueOf(invoiceRepository.count()));
        summary.put("payments", Long.valueOf(paymentRepository.count()));
        summary.put("notifications", Long.valueOf(notificationRepository.count()));
        summary.put("auditEvents", Long.valueOf(auditEventRepository.count()));
        summary.put("snapshots", Long.valueOf(reportSnapshotRepository.count()));

        // Same figures again, this time outside JPA.
        summary.put("overdueInvoicesRaw", Long.valueOf(databaseHelper.queryForLong(
                "select count(*) from invoices where status = ?", BillingService.INVOICE_OVERDUE)));
        summary.put("pendingNotificationsRaw", Long.valueOf(databaseHelper.queryForLong(
                "select count(*) from notifications where status = ?", LegacyUtils.STATUS_PENDING)));
        summary.put("closedTasksRaw", Long.valueOf(databaseHelper.queryForLong(
                "select count(*) from project_tasks where status = ?", TaskService.TASK_CLOSED)));

        summary.put("environment", commonService.describeEnvironment());

        LegacyUtils.recordDomainTouch("reporting", "generateOperationalSummary");
        return summary;
    }

    /**
     * Per-user workload view spanning identity, membership and task data.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> generateWorkloadReport() {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        List<UserRegister> users = userService.findAll();
        if (users == null) {
            return rows;
        }

        for (UserRegister user : users) {
            List<ProjectTask> tasks = projectTaskRepository.findByAssignedTo(user.getId());
            double actual = 0.0;
            int open = 0;
            for (ProjectTask task : tasks) {
                actual += task.getActualHours();
                if (!TaskService.TASK_CLOSED.equals(task.getStatus())) {
                    open++;
                }
            }

            Map<String, Object> row = new HashMap<String, Object>();
            row.put("userId", Integer.valueOf(user.getId()));
            row.put("userLabel", commonService.resolveUserLabel(user.getId()));
            row.put("memberships", Integer.valueOf(projectMemberRepository.findByUserId(user.getId()).size()));
            row.put("tasks", Integer.valueOf(tasks.size()));
            row.put("openTasks", Integer.valueOf(open));
            row.put("actualHours", Double.valueOf(actual));
            row.put("notifications", Integer.valueOf(notificationRepository.findByUserId(user.getId()).size()));
            rows.add(row);
        }
        return rows;
    }

    /**
     * Change-request analytics that walks both the change and project tables.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateChangeReport() {
        List<ChangeRequest> requests = changeRequestRepository.findAll();
        Map<String, Integer> byStatus = new HashMap<String, Integer>();
        Map<String, Integer> byProject = new HashMap<String, Integer>();

        for (ChangeRequest request : requests) {
            String status = request.getStatus() == null ? "UNKNOWN" : request.getStatus();
            Integer statusCount = byStatus.get(status);
            byStatus.put(status, Integer.valueOf(statusCount == null ? 1 : statusCount.intValue() + 1));

            String label = commonService.resolveProjectLabel(request.getProjectId());
            Integer projectCount = byProject.get(label);
            byProject.put(label, Integer.valueOf(projectCount == null ? 1 : projectCount.intValue() + 1));
        }

        Map<String, Object> report = new HashMap<String, Object>();
        report.put("total", Integer.valueOf(requests.size()));
        report.put("byStatus", byStatus);
        report.put("byProject", byProject);
        return report;
    }

    /**
     * Called from inside the billing transaction. Deliberately carries no
     * transaction annotation of its own so the snapshot commits or rolls back
     * with whatever billing is doing, and it calls back into the project
     * component while it is at it.
     */
    public ReportSnapshot captureBillingSnapshot(int projectId, String trigger, int actorId) {
        Map<String, Object> statusSummary = projectDomainService.getProjectStatusSummary(projectId);

        BigDecimal invoiced = LegacyUtils.safeAmount(invoiceRepository.sumAmountByProject(projectId));

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("trigger", trigger);
        values.put("projectStatus", statusSummary.get("status"));
        values.put("actualHours", statusSummary.get("actualHours"));
        values.put("hourlyRate", statusSummary.get("hourlyRate"));
        values.put("invoiced", invoiced);
        values.put("actorId", Integer.valueOf(actorId));

        LegacyUtils.recordDomainTouch("reporting", "captureBillingSnapshot");
        return persistSnapshot(projectId, REPORT_BILLING, "billing", values);
    }

    /**
     * Hands off to the original file based report writer.
     */
    public String exportLegacyUserCsv() {
        String csv = reportService.generateUserReport();
        LegacyUtils.recordDomainTouch("reporting", "exportLegacyUserCsv");
        return csv;
    }

    @Transactional(readOnly = true)
    public List<ReportSnapshot> findSnapshots(int projectId) {
        return reportSnapshotRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<ReportSnapshot> findAllSnapshots() {
        return reportSnapshotRepository.findAll();
    }

    private ReportSnapshot persistSnapshot(int projectId, String reportType, String generatedBy,
            Map<String, Object> values) {
        ReportSnapshot snapshot = new ReportSnapshot();
        snapshot.setProjectId(projectId);
        snapshot.setReportType(reportType);
        snapshot.setGeneratedBy(generatedBy == null ? "system" : generatedBy);
        snapshot.setSnapshotData(LegacyUtils.truncate(LegacyUtils.toLegacyPayload(values), 4000));
        snapshot.setCreatedAt(new Date());
        return reportSnapshotRepository.save(snapshot);
    }

    private ProjectRegister loadProject(int projectId) {
        Optional<ProjectRegister> optional = projectRepository.findById(Integer.valueOf(projectId));
        if (!optional.isPresent()) {
            throw new RuntimeException("Project not found for id " + projectId);
        }
        return optional.get();
    }
}
