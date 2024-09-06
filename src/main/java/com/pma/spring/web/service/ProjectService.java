package com.pma.spring.web.service;

import java.util.List;

import com.pma.spring.web.entity.ProjectRegister;

public interface ProjectService {
	
	ProjectRegister save(ProjectRegister project);
	
	List<ProjectRegister> findAll();
	
	ProjectRegister updateProject(int id);
	
	void deleteById(int id);

}
