package com.pma.spring.reporting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.billing.service.CreditNoteService;
import com.pma.spring.integration.service.WebhookSubscriptionService;
import com.pma.spring.reporting.model.ExecutiveSummary;
import com.pma.spring.support.DomainBenchmarkTestData;
import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.support.BenchmarkTestData;
import com.pma.spring.web.support.MonolithTest;

/**
 * Covers ExecutiveSummaryService, deliberately the highest fan-in component:
 * a single read spans project, task, invoice and credit-note data plus a
 * global webhook subscription count.
 */
@MonolithTest
class ExecutiveSummaryServiceTests {

    @Autowired
    private ExecutiveSummaryService executiveSummaryService;

    @Autowired
    private CreditNoteService creditNoteService;

    @Autowired
    private WebhookSubscriptionService webhookSubscriptionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    void summaryAggregatesTasksInvoicesCreditsAndWebhooks() {
        UserRegister owner = BenchmarkTestData.newUser(userRepository, "summary-owner");
        ProjectRegister project = BenchmarkTestData.newProject(projectRepository, "summary-proj");

        ProjectTask openTask = DomainBenchmarkTestData.newTask(projectTaskRepository, project.getProjectId(),
                owner.getId(), "summary-open-task");
        ProjectTask closedTask = DomainBenchmarkTestData.newTask(projectTaskRepository, project.getProjectId(),
                owner.getId(), "summary-closed-task");
        closedTask.setStatus("CLOSED");
        projectTaskRepository.save(closedTask);

        Invoice invoice = DomainBenchmarkTestData.newInvoice(invoiceRepository, project.getProjectId(),
                owner.getId(), new BigDecimal("800.00"));
        creditNoteService.issueCreditNote(invoice.getId(), new BigDecimal("50.00"), "summary test credit");

        long webhooksBefore = webhookSubscriptionService.countActive();
        webhookSubscriptionService.subscribe("https://example.test/hook", "SOME_EVENT");

        ExecutiveSummary summary = executiveSummaryService.summarize(project.getProjectId());

        assertEquals(project.getProjectId(), summary.getProjectId());
        assertTrue(summary.getOpenTaskCount() >= 1);
        assertEquals(0, new BigDecimal("800.00").compareTo(summary.getInvoicedTotal()));
        assertEquals((int) webhooksBefore + 1, summary.getActiveWebhookCount());
    }

    @Test
    void summaryForMissingProjectThrows() {
        assertThrows(RuntimeException.class, () -> executiveSummaryService.summarize(Integer.MAX_VALUE - 1));
    }
}
