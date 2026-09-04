package com.pma.spring.reporting.export;

import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pma.spring.web.entity.ReportSnapshot;
import com.pma.spring.web.repository.ReportSnapshotRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Renders a stored {@link ReportSnapshot} (owned by the original
 * com.pma.spring.web.service.ReportingService / ReportGenerationJob) as a
 * flat CSV line. Read-only against that repository - it adds an export
 * format on top of existing data rather than a new table.
 */
@Service
public class ReportExportService {

    private static final Logger logger = Logger.getLogger(ReportExportService.class);

    @Autowired
    private ReportSnapshotRepository reportSnapshotRepository;

    @Transactional(readOnly = true)
    public String exportSnapshotAsCsv(int snapshotId) {
        Optional<ReportSnapshot> snapshot = reportSnapshotRepository.findById(Integer.valueOf(snapshotId));
        if (!snapshot.isPresent()) {
            throw new RuntimeException("Report snapshot not found for id " + snapshotId);
        }

        ReportSnapshot entity = snapshot.get();
        StringBuilder csv = new StringBuilder();
        csv.append("id,projectId,reportType,generatedBy,createdAt\n");
        csv.append(entity.getId()).append(',');
        csv.append(entity.getProjectId()).append(',');
        csv.append(csvEscape(entity.getReportType())).append(',');
        csv.append(csvEscape(entity.getGeneratedBy())).append(',');
        csv.append(LegacyUtils.formatTimestamp(entity.getCreatedAt()));

        LegacyUtils.recordDomainTouch("reporting", "SNAPSHOT_EXPORTED");
        logger.info("Snapshot " + snapshotId + " exported as CSV");
        return csv.toString();
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
