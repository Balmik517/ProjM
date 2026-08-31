package com.pma.spring.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pma.spring.web.entity.ReportSnapshot;

@Repository
public interface ReportSnapshotRepository extends JpaRepository<ReportSnapshot, Integer> {

    List<ReportSnapshot> findByProjectId(int projectId);

    List<ReportSnapshot> findByReportType(String reportType);

    List<ReportSnapshot> findByProjectIdAndReportType(int projectId, String reportType);
}
