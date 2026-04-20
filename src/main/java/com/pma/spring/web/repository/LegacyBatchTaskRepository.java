package com.pma.spring.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pma.spring.web.entity.LegacyBatchTask;

@Repository
public interface LegacyBatchTaskRepository extends JpaRepository<LegacyBatchTask, Integer> {

    List<LegacyBatchTask> findByStatus(String status);

    List<LegacyBatchTask> findByOwner(String owner);
}
