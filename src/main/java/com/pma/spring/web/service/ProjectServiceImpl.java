package com.pma.spring.web.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.repository.ProjectRepository;

@Service
public class ProjectServiceImpl implements ProjectService {

	@Autowired
	ProjectRepository projectRepository;

	@Override
	public ProjectRegister save(ProjectRegister project) {
		ProjectRegister optionalSave = projectRepository.save(project);
		return optionalSave;
	}

	@Override
	public List<ProjectRegister> findAll() {
		// TODO Auto-generated method stub
		List<ProjectRegister> projectList = projectRepository.findAll();
		if (projectList.isEmpty()) {
			return null;
		}
		return projectList;
	}

	@Override
	public ProjectRegister updateProject(int id) {
		// TODO Auto-generated method stub

		Optional<ProjectRegister> optional = projectRepository.findById(id);
		ProjectRegister projectRegister = null;
		if (optional.isPresent()) {
			projectRegister = optional.get();
		} else {
			throw new RuntimeException("project not found for id " + id);
		}
		return projectRegister;
	}

	@Override
	public void deleteById(int id) {
		// TODO Auto-generated method stub
		projectRepository.deleteById(id);

	}

}
