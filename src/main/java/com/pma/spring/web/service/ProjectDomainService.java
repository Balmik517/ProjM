package com.pma.spring.web.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.web.entity.ProjectMember;
import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.ProjectMemberRepository;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.ProjectTaskRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Project lifecycle and membership.
 *
 * Complements the original {@link ProjectServiceImpl} CRUD service rather than
 * replacing it, so {@code project_register} now has more than one writer inside
 * the project domain alone.
 *
 * Note the outbound call to {@link BillingService}: project completion asks
 * billing to raise the final invoice, while billing in turn calls reporting and
 * reporting calls back here. That loop is deliberate.
 */
@Service
public class ProjectDomainService {

    private static final Logger logger = Logger.getLogger(ProjectDomainService.class);

    public static final String PROJECT_ACTIVE = "ACTIVE";
    public static final String PROJECT_ON_HOLD = "ON_HOLD";
    public static final String PROJECT_COMPLETED = "COMPLETED";
    public static final String PROJECT_CANCELLED = "CANCELLED";

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_MEMBER = "MEMBER";

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    /** Cross-domain read: the project component validating identity rows. */
    @Autowired
    private UserRepository userRepository;

    /** Cross-domain read: effort rollups for the project summary. */
    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    /** Original CRUD service, still used for plain persistence. */
    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private CommonService commonService;

    /**
     * Back-edge that closes the project -> billing -> reporting -> project loop.
     */
    @Autowired
    private BillingService billingService;

    /**
     * Simple cohesive transaction: create the project row and its owner
     * membership together. Both tables belong to the project domain.
     */
    @Transactional
    public ProjectRegister createProject(String projectName, Date startDate, Date endDate, int ownerUserId) {
        ProjectRegister project = new ProjectRegister();
        project.setProjectName(StringUtils.defaultIfBlank(projectName, "Untitled project"));
        project.setStartDate(startDate == null ? new Date() : startDate);
        project.setEndDate(endDate);
        project.setStatus(PROJECT_ACTIVE);

        ProjectRegister saved = projectService.save(project);

        if (ownerUserId > 0) {
            ProjectMember owner = new ProjectMember(saved.getProjectId(), ownerUserId, ROLE_OWNER,
                    LegacyUtils.STATUS_ACTIVE, new Date());
            projectMemberRepository.save(owner);
        }

        auditService.record(AuditService.ENTITY_PROJECT, saved.getProjectId(), "PROJECT_CREATED", ownerUserId,
                "name=" + saved.getProjectName());

        if (ownerUserId > 0) {
            notificationService.queue(ownerUserId, saved.getProjectId(),
                    NotificationService.TYPE_PROJECT_CREATED, "Project created: " + saved.getProjectName());
        }

        LegacyUtils.recordDomainTouch("project", "createProject");
        logger.info("Project " + saved.getProjectId() + " created with owner " + ownerUserId);
        return saved;
    }

    /**
     * Cross-domain transaction: membership plus identity read plus audit plus
     * notification, all inside one boundary.
     */
    @Transactional
    public ProjectMember assignMember(int projectId, int userId, String role, int actorId) {
        ProjectRegister project = loadProject(projectId);

        Optional<UserRegister> user = userRepository.findById(Integer.valueOf(userId));
        if (!user.isPresent()) {
            throw new RuntimeException("User not found for id " + userId);
        }

        List<ProjectMember> existing = projectMemberRepository.findByProjectIdAndUserId(projectId, userId);
        ProjectMember member;
        if (existing.isEmpty()) {
            member = new ProjectMember(projectId, userId, LegacyUtils.normalizeStatus(role, ROLE_MEMBER),
                    LegacyUtils.STATUS_ACTIVE, new Date());
        } else {
            member = existing.get(0);
            member.setRole(LegacyUtils.normalizeStatus(role, ROLE_MEMBER));
            member.setStatus(LegacyUtils.STATUS_ACTIVE);
        }

        ProjectMember saved = projectMemberRepository.save(member);

        auditService.record(AuditService.ENTITY_MEMBER, saved.getId(), "MEMBER_ASSIGNED", actorId,
                "projectId=" + projectId + ";userId=" + userId + ";role=" + saved.getRole());

        notificationService.queue(userId, projectId, NotificationService.TYPE_MEMBER_ASSIGNED,
                "You were added to " + project.getProjectName() + " as " + saved.getRole());

        LegacyUtils.recordDomainTouch("project", "assignMember");
        return saved;
    }

    @Transactional
    public ProjectMember removeMember(int projectId, int userId, int actorId) {
        List<ProjectMember> existing = projectMemberRepository.findByProjectIdAndUserId(projectId, userId);
        if (existing.isEmpty()) {
            throw new RuntimeException("Membership not found for project " + projectId + " user " + userId);
        }

        ProjectMember member = existing.get(0);
        member.setStatus("REMOVED");
        ProjectMember saved = projectMemberRepository.save(member);

        auditService.record(AuditService.ENTITY_MEMBER, saved.getId(), "MEMBER_REMOVED", actorId,
                "projectId=" + projectId + ";userId=" + userId);
        return saved;
    }

    @Transactional
    public ProjectRegister changeStatus(int projectId, String status, int actorId) {
        ProjectRegister project = loadProject(projectId);
        String previous = project.getStatus();
        String normalized = LegacyUtils.normalizeStatus(status, PROJECT_ACTIVE);

        project.setStatus(normalized);
        if (PROJECT_COMPLETED.equals(normalized)) {
            project.setCompletedAt(new Date());
        }
        ProjectRegister saved = projectRepository.save(project);

        auditService.record(AuditService.ENTITY_PROJECT, projectId, "PROJECT_STATUS_CHANGED", actorId,
                "from=" + previous + ";to=" + normalized);

        LegacyUtils.recordDomainTouch("project", "changeStatus");
        return saved;
    }

    /**
     * Close out a project and immediately hand off to billing so the final
     * invoice is raised in the same transaction. This is where the project
     * domain reaches into the billing domain.
     */
    @Transactional
    public Map<String, Object> closeOutProject(int projectId, int actorId) {
        ProjectRegister project = loadProject(projectId);
        project.setStatus(PROJECT_COMPLETED);
        project.setCompletedAt(new Date());
        projectRepository.save(project);

        BigDecimal invoiced = billingService.generateFinalInvoiceForProject(projectId, actorId);

        auditService.record(AuditService.ENTITY_PROJECT, projectId, "PROJECT_CLOSED_OUT", actorId,
                "invoiced=" + invoiced);

        notificationService.queueForProjectOwner(projectId, NotificationService.TYPE_PROJECT_COMPLETED,
                "Project closed out and invoiced");

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("projectId", Integer.valueOf(projectId));
        result.put("status", project.getStatus());
        result.put("invoicedAmount", invoiced);
        return result;
    }

    @Transactional(readOnly = true)
    public List<ProjectMember> findMembers(int projectId) {
        return projectMemberRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<ProjectMember> findProjectsForUser(int userId) {
        return projectMemberRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public ProjectRegister findById(int projectId) {
        return loadProject(projectId);
    }

    @Transactional(readOnly = true)
    public List<ProjectRegister> findAll() {
        return projectRepository.findAll();
    }

    /**
     * Status rollup consumed by the reporting component, which is what makes
     * the dependency loop close.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProjectStatusSummary(int projectId) {
        ProjectRegister project = loadProject(projectId);

        Double actual = projectTaskRepository.sumActualHoursByProject(projectId);
        Double estimated = projectTaskRepository.sumEstimatedHoursByProject(projectId);

        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("projectId", Integer.valueOf(projectId));
        summary.put("projectName", project.getProjectName());
        summary.put("status", StringUtils.defaultIfBlank(project.getStatus(), PROJECT_ACTIVE));
        summary.put("startDate", LegacyUtils.formatTimestamp(project.getStartDate()));
        summary.put("endDate", LegacyUtils.formatTimestamp(project.getEndDate()));
        summary.put("completedAt", LegacyUtils.formatTimestamp(project.getCompletedAt()));
        summary.put("activeMembers",
                Integer.valueOf(projectMemberRepository.findByProjectIdAndStatus(projectId,
                        LegacyUtils.STATUS_ACTIVE).size()));
        summary.put("openTasks", Long.valueOf(projectTaskRepository.countOpenTasks(projectId)));
        summary.put("actualHours", Double.valueOf(actual == null ? 0.0 : actual.doubleValue()));
        summary.put("estimatedHours", Double.valueOf(estimated == null ? 0.0 : estimated.doubleValue()));
        summary.put("hourlyRate", Double.valueOf(commonService.resolveHourlyRate(projectId)));
        return summary;
    }

    /**
     * Membership view enriched with identity data pulled through the original
     * user service.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMemberDirectory(int projectId) {
        List<Map<String, Object>> directory = new java.util.ArrayList<Map<String, Object>>();
        for (ProjectMember member : projectMemberRepository.findByProjectId(projectId)) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("memberId", Integer.valueOf(member.getId()));
            row.put("userId", Integer.valueOf(member.getUserId()));
            row.put("role", member.getRole());
            row.put("status", member.getStatus());
            row.put("joinedAt", LegacyUtils.formatTimestamp(member.getJoinedAt()));
            row.put("userLabel", commonService.resolveUserLabel(member.getUserId()));
            directory.add(row);
        }
        return directory;
    }

    /**
     * Uses the original user service so the legacy identity lookups stay on the
     * project component's dependency list.
     */
    @Transactional(readOnly = true)
    public UserRegister resolveOwner(int projectId) {
        List<ProjectMember> owners = projectMemberRepository.findByProjectId(projectId);
        for (ProjectMember member : owners) {
            if (ROLE_OWNER.equals(member.getRole())) {
                for (UserRegister user : safeUserList()) {
                    if (user.getId() == member.getUserId()) {
                        return user;
                    }
                }
            }
        }
        return null;
    }

    private List<UserRegister> safeUserList() {
        List<UserRegister> users = userService.findAll();
        return users == null ? new java.util.ArrayList<UserRegister>() : users;
    }

    private ProjectRegister loadProject(int projectId) {
        Optional<ProjectRegister> optional = projectRepository.findById(Integer.valueOf(projectId));
        if (!optional.isPresent()) {
            throw new RuntimeException("Project not found for id " + projectId);
        }
        return optional.get();
    }
}
