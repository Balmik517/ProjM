package com.pma.spring.web.controller;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.web.dto.ProjectMemberRequest;
import com.pma.spring.web.entity.ProjectMember;
import com.pma.spring.web.service.ProjectDomainService;

/**
 * Membership endpoints for a project.
 */
@RestController
@RequestMapping("/projects")
public class ProjectMemberController {

    private static final Logger logger = Logger.getLogger(ProjectMemberController.class);

    @Autowired
    private ProjectDomainService projectDomainService;

    @GetMapping("/{id}/members")
    public ResponseEntity<List<ProjectMember>> listMembers(@PathVariable("id") int projectId) {
        return ResponseEntity.ok(projectDomainService.findMembers(projectId));
    }

    @GetMapping("/{id}/members/directory")
    public ResponseEntity<List<Map<String, Object>>> memberDirectory(@PathVariable("id") int projectId) {
        return ResponseEntity.ok(projectDomainService.getMemberDirectory(projectId));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ProjectMember> assignMember(@PathVariable("id") int projectId,
            @RequestBody ProjectMemberRequest request) {
        try {
            ProjectMember member = projectDomainService.assignMember(projectId, request.getUserId(),
                    request.getRole(), request.getActorId());
            return ResponseEntity.ok(member);
        } catch (RuntimeException e) {
            logger.warn("Failed to assign member on project " + projectId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ProjectMember> removeMember(@PathVariable("id") int projectId,
            @PathVariable("userId") int userId,
            @RequestParam(value = "actorId", defaultValue = "0") int actorId) {
        try {
            return ResponseEntity.ok(projectDomainService.removeMember(projectId, userId, actorId));
        } catch (RuntimeException e) {
            logger.warn("Failed to remove member: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> projectStatus(@PathVariable("id") int projectId) {
        try {
            return ResponseEntity.ok(projectDomainService.getProjectStatusSummary(projectId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
