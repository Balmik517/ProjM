package com.pma.spring.workflow.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.workflow.service.ProjectCompletionWorkflowService;

@RestController
@RequestMapping("/workflow/projects")
public class ProjectCompletionController {

    @Autowired
    private ProjectCompletionWorkflowService projectCompletionWorkflowService;

    @PostMapping("/{projectId}/complete-full")
    public ResponseEntity<Map<String, Object>> completeFull(@PathVariable("projectId") int projectId,
            @RequestParam("actorId") int actorId) {
        try {
            return ResponseEntity.ok(projectCompletionWorkflowService.completeProject(projectId, actorId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
