package com.pma.spring.web.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pma.spring.web.entity.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    List<Payment> findByInvoiceId(int invoiceId);

    List<Payment> findByStatus(String status);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.invoiceId = :invoiceId and p.status = 'SETTLED'")
    BigDecimal sumSettledByInvoice(@Param("invoiceId") int invoiceId);
}
