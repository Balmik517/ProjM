package com.pma.spring.web.scheduler;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.pma.spring.web.service.ReportService;
import com.pma.spring.web.util.ServiceLocator;

/**
 * Quartz Job for periodic report generation.
 * Uses ServiceLocator anti-pattern to get Spring beans (since Quartz jobs
 * are not managed by Spring by default in this legacy setup).
 */
public class ReportGenerationJob implements Job {

    private static final Logger logger = Logger.getLogger(ReportGenerationJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        logger.info("ReportGenerationJob started at " + timestamp);

        try {
            // Use ServiceLocator anti-pattern to get Spring bean
            ReportService reportService = ServiceLocator.getService("reportService");

            if (reportService != null) {
                // Generate user report
                String userReport = reportService.generateUserReport();
                reportService.saveReportToFile(userReport, "daily_users");
                logger.info("Daily user report generated");

                // Generate HTML report
                String htmlReport = reportService.generateHtmlReport();
                reportService.saveReportToFile(htmlReport, "daily_html");
                logger.info("Daily HTML report generated");
            } else {
                logger.warn("ReportService not available via ServiceLocator");
            }
        } catch (Exception e) {
            logger.error("ReportGenerationJob failed", e);
            throw new JobExecutionException("Report generation failed", e);
        }
    }
}
