package com.pma.spring.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pma.spring.web.entity.ProjectTask;

@Repository
public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Integer> {

    List<ProjectTask> findByProjectId(int projectId);

    List<ProjectTask> findByAssignedTo(int assignedTo);

    List<ProjectTask> findByProjectIdAndStatus(int projectId, String status);

    List<ProjectTask> findByStatus(String status);

    /**
     * Effort rollup consumed by both billing and reporting.
     */
    @Query("select coalesce(sum(t.actualHours), 0) from ProjectTask t where t.projectId = :projectId")
    Double sumActualHoursByProject(@Param("projectId") int projectId);

    @Query("select coalesce(sum(t.estimatedHours), 0) from ProjectTask t where t.projectId = :projectId")
    Double sumEstimatedHoursByProject(@Param("projectId") int projectId);

    @Query("select count(t) from ProjectTask t where t.projectId = :projectId and t.status <> 'CLOSED'")
    long countOpenTasks(@Param("projectId") int projectId);
}
