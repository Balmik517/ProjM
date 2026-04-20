package com.pma.spring.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pma.spring.web.entity.ProjectRegister;


@Repository
public interface ProjectRepository  extends JpaRepository<ProjectRegister, Integer> {

}
