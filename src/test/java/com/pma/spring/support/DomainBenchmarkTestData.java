package com.pma.spring.support;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import com.pma.spring.web.entity.Invoice;
import com.pma.spring.web.entity.ProjectMember;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.repository.InvoiceRepository;
import com.pma.spring.web.repository.ProjectMemberRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;

/**
 * Test-data builders for the new domain-oriented packages (task, billing,
 * notification, audit, reporting, integration, workflow).
 *
 * Deliberately separate from
 * {@link com.pma.spring.web.support.BenchmarkTestData}, which is left
 * untouched, even though the two overlap conceptually - tests for the new
 * packages still need users and projects, so they reuse that class directly,
 * and only add builders for the additional entities (tasks, invoices,
 * members) those tests also require.
 */
public final class DomainBenchmarkTestData {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private DomainBenchmarkTestData() {
    }

    public static ProjectTask newTask(ProjectTaskRepository projectTaskRepository, int projectId, int assignedTo,
            String label) {
        int seq = SEQUENCE.incrementAndGet();
        ProjectTask task = new ProjectTask();
        task.setProjectId(projectId);
        task.setAssignedTo(assignedTo);
        task.setTitle(label + "-" + seq);
        task.setStatus("OPEN");
        task.setPriority("MEDIUM");
        task.setEstimatedHours(4.0);
        task.setActualHours(0.0);
        task.setCreatedAt(new Date());
        task.setUpdatedAt(new Date());
        return projectTaskRepository.save(task);
    }

    public static Invoice newInvoice(InvoiceRepository invoiceRepository, int projectId, int customerId,
            BigDecimal amount) {
        int seq = SEQUENCE.incrementAndGet();
        Invoice invoice = new Invoice();
        invoice.setProjectId(projectId);
        invoice.setCustomerId(customerId);
        invoice.setAmount(amount);
        invoice.setStatus("ISSUED");
        invoice.setReference("TEST-INV-" + seq);
        invoice.setIssuedAt(new Date());
        invoice.setDueAt(new Date());
        return invoiceRepository.save(invoice);
    }

    public static ProjectMember newMember(ProjectMemberRepository projectMemberRepository, int projectId,
            int userId, String role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(role);
        member.setStatus("ACTIVE");
        member.setJoinedAt(new Date());
        return projectMemberRepository.save(member);
    }
}
