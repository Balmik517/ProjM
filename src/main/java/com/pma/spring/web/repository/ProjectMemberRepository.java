package com.pma.spring.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pma.spring.web.entity.ProjectMember;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Integer> {

    List<ProjectMember> findByProjectId(int projectId);

    List<ProjectMember> findByUserId(int userId);

    List<ProjectMember> findByProjectIdAndStatus(int projectId, String status);

    List<ProjectMember> findByProjectIdAndUserId(int projectId, int userId);
}
