package com.pma.spring.reporting.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pma.spring.reporting.model.ExecutiveSummary;
import com.pma.spring.reporting.service.ExecutiveSummaryService;

@RestController
@RequestMapping("/reporting/executive-summary")
public class ExecutiveSummaryController {

    @Autowired
    private ExecutiveSummaryService executiveSummaryService;

    @GetMapping("/{projectId}")
    public ResponseEntity<ExecutiveSummary> summarize(@PathVariable("projectId") int projectId) {
        try {
            return ResponseEntity.ok(executiveSummaryService.summarize(projectId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
