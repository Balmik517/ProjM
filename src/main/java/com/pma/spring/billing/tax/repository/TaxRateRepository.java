package com.pma.spring.billing.tax.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pma.spring.billing.tax.entity.TaxRate;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, Integer> {

    Optional<TaxRate> findByRegion(String region);
}
