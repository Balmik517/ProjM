package com.pma.spring.workflow.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.billing.service.CreditNoteService;
import com.pma.spring.integration.entity.WebhookSubscription;
import com.pma.spring.integration.service.WebhookSubscriptionService;
import com.pma.spring.integration.util.WebhookPayloadUtil;
import com.pma.spring.reporting.model.ExecutiveSummary;
import com.pma.spring.reporting.service.ExecutiveSummaryService;
import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.ProjectMember;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.entity.ReportSnapshot;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.repository.ProjectMemberRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.repository.ReportSnapshotRepository;
import com.pma.spring.web.service.AuditService;
import com.pma.spring.web.service.NotificationService;
import com.pma.spring.web.util.LegacyUtils;

/**
 * A second, independent way to "complete a project".
 *
 * {@link com.pma.spring.web.service.ProjectDomainService#closeOutProject}
 * already does this - project status, a final invoice, one audit row, one
 * notification - and is left completely untouched. This class does not call
 * it and does not replace it; it is a parallel, fuller workflow that grew up
 * in the new domain packages and updates {@code project_register} directly
 * through the same {@link ProjectRepository}. Two independent components
 * that can each mark a project COMPLETED, disagreeing on what else that
 * should trigger, is exactly the kind of consistency risk a real monolith
 * accumulates when a domain gets touched from more than one place over time.
 *
 * This class grew across three commits, each widening the single
 * {@code @Transactional} method to reach one more package: first project and
 * task, then notification and billing, then reporting and integration (this
 * revision). The finished method now touches seven packages - project, task,
 * audit, notification, billing, reporting and integration - in one
 * transaction, which is the single broadest write-path in the application.
 */
@Service
public class ProjectCompletionWorkflowService {

    private static final Logger logger = Logger.getLogger(ProjectCompletionWorkflowService.class);

    private static final String EVENT_PROJECT_COMPLETED = "PROJECT_COMPLETED";

    private static final int SYSTEM_ACTOR_ID = 0;

    public static final String STATUS_COMPLETED = "COMPLETED";

    @Autowired
    private ProjectRepository projectRepository;

    /** Cross-package write: every open task on the project is force-closed. */
    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private AuditService auditService;

    /** Cross-package write: every active member is notified the project closed. */
    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private NotificationService notificationService;

    /** Cross-package read: outstanding balance is computed net of credit notes. */
    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CreditNoteService creditNoteService;

    /** Cross-package read: a completion snapshot is generated from the same executive summary reporting uses. */
    @Autowired
    private ExecutiveSummaryService executiveSummaryService;

    @Autowired
    private ReportSnapshotRepository reportSnapshotRepository;

    /** Cross-package read+write: any subscriber to PROJECT_COMPLETED gets a dispatch record. */
    @Autowired
    private WebhookSubscriptionService webhookSubscriptionService;

    @Transactional
    public Map<String, Object> completeProject(int projectId, int actorId) {
        Optional<ProjectRegister> projectLookup = projectRepository.findById(Integer.valueOf(projectId));
        if (!projectLookup.isPresent()) {
            throw new RuntimeException("Project not found for id " + projectId);
        }
        ProjectRegister project = projectLookup.get();
        if (STATUS_COMPLETED.equals(project.getStatus())) {
            throw new RuntimeException("Project " + projectId + " is already completed");
        }

        project.setStatus(STATUS_COMPLETED);
        project.setCompletedAt(new Date());
        projectRepository.save(project);

        int closedTaskCount = 0;
        List<ProjectTask> tasks = projectTaskRepository.findByProjectId(projectId);
        for (ProjectTask task : tasks) {
            if (!LegacyUtils.STATUS_CLOSED.equals(task.getStatus())) {
                task.setStatus(LegacyUtils.STATUS_CLOSED);
                task.setUpdatedAt(new Date());
                projectTaskRepository.save(task);
                closedTaskCount++;
            }
        }

        auditService.record(AuditService.ENTITY_PROJECT, projectId, "PROJECT_COMPLETED_FULL_WORKFLOW", actorId,
                "closedTasks=" + closedTaskCount);
        LegacyUtils.recordDomainTouch("workflow", "PROJECT_COMPLETION");

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        for (ProjectMember member : members) {
            notificationService.queue(member.getUserId(), projectId, NotificationService.TYPE_PROJECT_COMPLETED,
                    "Project \"" + LegacyUtils.truncate(project.getProjectName(), 100) + "\" has been completed");
        }

        BigDecimal outstanding = BigDecimal.ZERO;
        List<Invoice> invoices = invoiceRepository.findByProjectId(projectId);
        for (Invoice invoice : invoices) {
            outstanding = outstanding.add(creditNoteService.outstandingAfterCredits(invoice.getId()));
        }
        if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
            auditService.record(AuditService.ENTITY_PROJECT, projectId, "PROJECT_COMPLETED_WITH_BALANCE_DUE",
                    actorId, "outstanding=" + outstanding);
        }

        ExecutiveSummary summary = executiveSummaryService.summarize(projectId);
        ReportSnapshot snapshot = new ReportSnapshot();
        snapshot.setProjectId(projectId);
        snapshot.setReportType("PROJECT_COMPLETION");
        snapshot.setGeneratedBy("ProjectCompletionWorkflowService");
        snapshot.setSnapshotData("openTasks=" + summary.getOpenTaskCount() + ";invoiced=" + summary.getInvoicedTotal()
                + ";credited=" + summary.getCreditedTotal() + ";activeWebhooks=" + summary.getActiveWebhookCount());
        snapshot.setCreatedAt(new Date());
        reportSnapshotRepository.save(snapshot);

        List<WebhookSubscription> subscriptions = webhookSubscriptionService.findActiveFor(EVENT_PROJECT_COMPLETED);
        for (WebhookSubscription subscription : subscriptions) {
            auditService.record(AuditService.ENTITY_PROJECT, projectId, "WEBHOOK_DISPATCHED", SYSTEM_ACTOR_ID,
                    WebhookPayloadUtil.formatEventLine(EVENT_PROJECT_COMPLETED, subscription.getTargetUrl()));
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("projectId", Integer.valueOf(projectId));
        result.put("status", project.getStatus());
        result.put("closedTaskCount", Integer.valueOf(closedTaskCount));
        result.put("notifiedMemberCount", Integer.valueOf(members.size()));
        result.put("outstandingBalance", outstanding);
        result.put("webhookDispatchCount", Integer.valueOf(subscriptions.size()));

        logger.info("Project " + projectId + " completed via full workflow, closed " + closedTaskCount + " task(s)");
        return result;
    }
}
