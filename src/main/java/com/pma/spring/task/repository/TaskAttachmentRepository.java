package com.pma.spring.task.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pma.spring.task.entity.TaskAttachment;

@Repository
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Integer> {

    List<TaskAttachment> findByTaskId(int taskId);

    long countByTaskId(int taskId);
}
