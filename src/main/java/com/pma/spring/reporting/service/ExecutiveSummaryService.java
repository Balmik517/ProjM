package com.pma.spring.reporting.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.billing.entity.CreditNote;
import com.pma.spring.billing.repository.CreditNoteRepository;
import com.pma.spring.reporting.model.ExecutiveSummary;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.repository.IntegrationRequestRepository;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.repository.NotificationRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Deliberately the most coupled component added so far.
 *
 * A single read touches: {@code project_register}, {@code project_tasks},
 * {@code invoices}, {@code notifications} and {@code integration_requests}
 * (all still in {@code com.pma.spring.web}) plus {@code credit_notes} (the
 * new {@code com.pma.spring.billing} package). This mirrors the doc's Phase 5
 * guidance almost exactly: Reporting should read from everywhere, on
 * purpose, so it becomes the highly-coupled candidate in the dependency
 * graph rather than a clean extraction target.
 */
@Service
public class ExecutiveSummaryService {

    private static final Logger logger = Logger.getLogger(ExecutiveSummaryService.class);

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private IntegrationRequestRepository integrationRequestRepository;

    @Autowired
    private CreditNoteRepository creditNoteRepository;

    @Transactional(readOnly = true)
    public ExecutiveSummary summarize(int projectId) {
        Optional<ProjectRegister> project = projectRepository.findById(Integer.valueOf(projectId));
        if (!project.isPresent()) {
            throw new RuntimeException("Project not found for id " + projectId);
        }
        ProjectRegister projectEntity = project.get();

        ExecutiveSummary summary = new ExecutiveSummary();
        summary.setProjectId(projectId);
        summary.setProjectName(projectEntity.getProjectName());
        summary.setProjectStatus(projectEntity.getStatus());
        summary.setOpenTaskCount(projectTaskRepository.countOpenTasks(projectId));

        BigDecimal invoiced = LegacyUtils.safeAmount(invoiceRepository.sumAmountByProject(projectId));
        summary.setInvoicedTotal(invoiced);

        BigDecimal credited = BigDecimal.ZERO;
        List<CreditNote> creditNotes = creditNoteRepository.findByStatus(CreditNote.STATUS_ISSUED);
        for (CreditNote creditNote : creditNotes) {
            if (invoiceRepository.findByProjectId(projectId).stream()
                    .anyMatch(invoice -> invoice.getId() == creditNote.getInvoiceId())) {
                credited = credited.add(LegacyUtils.safeAmount(creditNote.getAmount()));
            }
        }
        summary.setCreditedTotal(credited);

        summary.setPendingNotificationCount(notificationRepository.findByProjectId(projectId).stream()
                .filter(n -> LegacyUtils.STATUS_PENDING.equals(n.getStatus())).count());

        summary.setIntegrationRequestCount(integrationRequestRepository.findByProjectId(projectId).size());

        LegacyUtils.recordDomainTouch("reporting", "EXECUTIVE_SUMMARY");
        logger.info("Executive summary generated for project " + projectId);
        return summary;
    }
}
