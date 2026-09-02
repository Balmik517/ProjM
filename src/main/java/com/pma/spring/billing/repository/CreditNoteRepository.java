package com.pma.spring.billing.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pma.spring.billing.entity.CreditNote;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Integer> {

    List<CreditNote> findByInvoiceId(int invoiceId);

    List<CreditNote> findByStatus(String status);

    @Query("select coalesce(sum(c.amount), 0) from CreditNote c where c.invoiceId = :invoiceId and c.status <> 'VOID'")
    BigDecimal sumActiveByInvoice(@Param("invoiceId") int invoiceId);
}
