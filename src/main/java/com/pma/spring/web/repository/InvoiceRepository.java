package com.pma.spring.web.repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pma.spring.web.entity.Invoice;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    List<Invoice> findByProjectId(int projectId);

    List<Invoice> findByCustomerId(int customerId);

    List<Invoice> findByStatus(String status);

    List<Invoice> findByStatusAndDueAtBefore(String status, Date cutoff);

    @Query("select coalesce(sum(i.amount), 0) from Invoice i where i.projectId = :projectId")
    BigDecimal sumAmountByProject(@Param("projectId") int projectId);

    /**
     * Native cross-table rollup over the billing tables. Reporting calls this
     * directly instead of going through the billing component, which hides the
     * dependency from a pure service-call graph.
     */
    @Query(value = "select i.status as invoice_status, count(p.id) as payment_count, "
            + "coalesce(sum(p.amount), 0) as settled "
            + "from invoices i left join payments p on p.invoice_id = i.id "
            + "where i.project_id = :projectId group by i.status", nativeQuery = true)
    List<Object[]> settlementBreakdownByProject(@Param("projectId") int projectId);
}
