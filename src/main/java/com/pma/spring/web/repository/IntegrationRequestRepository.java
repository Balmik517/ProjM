package com.pma.spring.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pma.spring.web.entity.IntegrationRequest;

@Repository
public interface IntegrationRequestRepository extends JpaRepository<IntegrationRequest, Integer> {

    List<IntegrationRequest> findByStatus(String status);

    List<IntegrationRequest> findByProjectId(int projectId);

    List<IntegrationRequest> findByIntegrationType(String integrationType);

    List<IntegrationRequest> findByStatusAndRetryCountLessThan(String status, int maxRetries);
}
