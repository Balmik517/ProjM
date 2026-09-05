package com.pma.spring.workflow.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.ProjectTask;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.service.AuditService;
import com.pma.spring.web.util.LegacyUtils;

/**
 * A second, independent way to "complete a project".
 *
 * {@link com.pma.spring.web.service.ProjectDomainService#closeOutProject}
 * already does this - project status, a final invoice, one audit row, one
 * notification - and is left completely untouched. This class does not call
 * it and does not replace it; it is a parallel, fuller workflow that grew up
 * in the new domain packages and updates {@code project_register} directly
 * through the same {@link ProjectRepository}. Two independent components
 * that can each mark a project COMPLETED, disagreeing on what else that
 * should trigger, is exactly the kind of consistency risk a real monolith
 * accumulates when a domain gets touched from more than one place over time.
 *
 * This class itself grows across three commits, each widening the single
 * {@code @Transactional} method to reach one more package: first project and
 * task, then notification and billing, then reporting and integration.
 */
@Service
public class ProjectCompletionWorkflowService {

    private static final Logger logger = Logger.getLogger(ProjectCompletionWorkflowService.class);

    public static final String STATUS_COMPLETED = "COMPLETED";

    @Autowired
    private ProjectRepository projectRepository;

    /** Cross-package write: every open task on the project is force-closed. */
    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public Map<String, Object> completeProject(int projectId, int actorId) {
        Optional<ProjectRegister> projectLookup = projectRepository.findById(Integer.valueOf(projectId));
        if (!projectLookup.isPresent()) {
            throw new RuntimeException("Project not found for id " + projectId);
        }
        ProjectRegister project = projectLookup.get();
        if (STATUS_COMPLETED.equals(project.getStatus())) {
            throw new RuntimeException("Project " + projectId + " is already completed");
        }

        project.setStatus(STATUS_COMPLETED);
        project.setCompletedAt(new Date());
        projectRepository.save(project);

        int closedTaskCount = 0;
        List<ProjectTask> tasks = projectTaskRepository.findByProjectId(projectId);
        for (ProjectTask task : tasks) {
            if (!LegacyUtils.STATUS_CLOSED.equals(task.getStatus())) {
                task.setStatus(LegacyUtils.STATUS_CLOSED);
                task.setUpdatedAt(new Date());
                projectTaskRepository.save(task);
                closedTaskCount++;
            }
        }

        auditService.record(AuditService.ENTITY_PROJECT, projectId, "PROJECT_COMPLETED_FULL_WORKFLOW", actorId,
                "closedTasks=" + closedTaskCount);
        LegacyUtils.recordDomainTouch("workflow", "PROJECT_COMPLETION");

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("projectId", Integer.valueOf(projectId));
        result.put("status", project.getStatus());
        result.put("closedTaskCount", Integer.valueOf(closedTaskCount));

        logger.info("Project " + projectId + " completed via full workflow, closed " + closedTaskCount + " task(s)");
        return result;
    }
}
