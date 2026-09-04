package com.pma.spring.audit.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pma.spring.audit.entity.AuditRetentionPolicy;

@Repository
public interface AuditRetentionPolicyRepository extends JpaRepository<AuditRetentionPolicy, Integer> {

    List<AuditRetentionPolicy> findAll();

    Optional<AuditRetentionPolicy> findByEntityType(String entityType);
}
