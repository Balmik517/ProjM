package com.pma.spring.reporting.export;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pma.spring.web.entity.ReportSnapshot;
import com.pma.spring.web.repository.ReportSnapshotRepository;
import com.pma.spring.web.support.MonolithTest;

/**
 * Covers ReportExportService, a purely read-only CSV formatter on top of the
 * original ReportSnapshotRepository.
 */
@MonolithTest
class ReportExportServiceTests {

    @Autowired
    private ReportExportService reportExportService;

    @Autowired
    private ReportSnapshotRepository reportSnapshotRepository;

    @Test
    void exportsSnapshotAsCsvWithHeaderAndEscapedValues() {
        ReportSnapshot snapshot = new ReportSnapshot();
        snapshot.setProjectId(1);
        snapshot.setReportType("TEST, TYPE");
        snapshot.setGeneratedBy("export-test");
        snapshot.setSnapshotData("irrelevant");
        snapshot.setCreatedAt(new Date());
        ReportSnapshot saved = reportSnapshotRepository.save(snapshot);

        String csv = reportExportService.exportSnapshotAsCsv(saved.getId());

        assertTrue(csv.startsWith("id,projectId,reportType,generatedBy,createdAt"));
        assertTrue(csv.contains("\"TEST, TYPE\""));
    }

    @Test
    void exportOfMissingSnapshotThrows() {
        assertThrows(RuntimeException.class, () -> reportExportService.exportSnapshotAsCsv(Integer.MAX_VALUE - 1));
    }
}
