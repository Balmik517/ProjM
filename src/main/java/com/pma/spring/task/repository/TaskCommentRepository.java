package com.pma.spring.task.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pma.spring.task.entity.TaskComment;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, Integer> {

    List<TaskComment> findByTaskId(int taskId);

    long countByTaskId(int taskId);
}
