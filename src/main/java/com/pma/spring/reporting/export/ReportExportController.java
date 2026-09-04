package com.pma.spring.reporting.export;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reporting/export")
public class ReportExportController {

    @Autowired
    private ReportExportService reportExportService;

    @GetMapping(value = "/{snapshotId}/csv")
    public ResponseEntity<String> csv(@PathVariable("snapshotId") int snapshotId) {
        try {
            return ResponseEntity.ok(reportExportService.exportSnapshotAsCsv(snapshotId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
