package com.pma.spring.web.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.UserRegister;

/**
 * Report generation service using legacy Apache Commons libraries.
 * - commons-collections 3.x (has known deserialization vulnerabilities)
 * - commons-lang 2.x (not commons-lang3)
 * - commons-io 2.4 (old version)
 * - commons-beanutils (has known vulnerabilities)
 * - log4j 1.x (CVE-2019-17571 and others)
 */
@Service
public class ReportService {

    private static final Logger logger = Logger.getLogger(ReportService.class);

    private static final String REPORT_DIR = "reports";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private ProjectServiceImpl projectService;

    /**
     * Generate a CSV report of all users using commons-lang 2.x.
     */
    public String generateUserReport() {
        List<UserRegister> users = userService.findAll();
        if (users == null || users.isEmpty()) {
            return "No users found";
        }

        StringBuilder report = new StringBuilder();
        report.append("ID,Name,Email,Phone,Address,DOB\n");

        for (UserRegister user : users) {
            // Using commons-lang 2.x StringEscapeUtils (deprecated in lang3)
            report.append(user.getId()).append(",");
            report.append(StringEscapeUtils.escapeCsv(StringUtils.defaultString(user.getName()))).append(",");
            report.append(StringEscapeUtils.escapeCsv(StringUtils.defaultString(user.getEmail()))).append(",");
            report.append(StringEscapeUtils.escapeCsv(StringUtils.defaultString(user.getPhoneNumber()))).append(",");
            report.append(StringEscapeUtils.escapeCsv(StringUtils.defaultString(user.getAddress()))).append(",");
            report.append(user.getDob() != null ? DateFormatUtils.format(user.getDob(), "yyyy-MM-dd") : "N/A");
            report.append("\n");
        }

        logger.info("Generated user report with " + users.size() + " entries");
        return report.toString();
    }

    /**
     * Generate project summary using commons-collections 3.x.
     * Uses raw types and deprecated Transformer interface.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<String, Object> generateProjectSummary() {
        List<ProjectRegister> projects = projectService.findAll();
        Map<String, Object> summary = new HashMap<>();

        if (projects == null || CollectionUtils.isEmpty(projects)) {
            summary.put("totalProjects", 0);
            return summary;
        }

        summary.put("totalProjects", projects.size());

        // Using commons-collections 3.x Transformer (raw types)
        Collection projectNames = CollectionUtils.collect(projects, new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((ProjectRegister) input).getProjectName();
            }
        });

        summary.put("projectNames", projectNames);

        // Using commons-collections MapUtils
        logger.info("Project summary: " + MapUtils.toProperties(summary));

        return summary;
    }

    /**
     * Copy user data using commons-beanutils (has CVE vulnerabilities).
     */
    public UserRegister cloneUser(UserRegister source) {
        UserRegister target = new UserRegister();
        try {
            // BeanUtils.copyProperties has known security issues
            BeanUtils.copyProperties(target, source);
            logger.info("User cloned using BeanUtils: " + source.getName());
        } catch (Exception e) {
            logger.error("Failed to clone user", e);
            throw new RuntimeException("Clone failed", e);
        }
        return target;
    }

    /**
     * Save report to file using commons-io.
     */
    public String saveReportToFile(String reportContent, String reportName) {
        try {
            File reportDir = new File(REPORT_DIR);
            if (!reportDir.exists()) {
                reportDir.mkdirs();
            }

            String fileName = reportName + "_" + DATE_FORMAT.format(new Date()) + ".csv";
            File reportFile = new File(reportDir, fileName);

            // Using commons-io FileUtils
            FileUtils.writeStringToFile(reportFile, reportContent);

            logger.info("Report saved to: " + reportFile.getAbsolutePath());
            return reportFile.getAbsolutePath();
        } catch (IOException e) {
            logger.error("Failed to save report", e);
            throw new RuntimeException("Report save failed", e);
        }
    }

    /**
     * Read report from file using commons-io.
     */
    public String readReport(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }

            // Using commons-io IOUtils
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(fis, baos);
            fis.close();

            return baos.toString("UTF-8");
        } catch (IOException e) {
            logger.error("Failed to read report: " + filePath, e);
            return null;
        }
    }

    /**
     * Generate HTML report using string concatenation (legacy pattern).
     */
    public String generateHtmlReport() {
        List<UserRegister> users = userService.findAll();
        List<ProjectRegister> projects = projectService.findAll();

        // String concatenation in loop (performance anti-pattern)
        String html = "<html><head><title>System Report</title></head><body>";
        html += "<h1>Project Management System Report</h1>";
        html += "<h2>Generated: " + new Date().toString() + "</h2>";

        html += "<h3>Users (" + (users != null ? users.size() : 0) + ")</h3>";
        html += "<table border='1'><tr><th>ID</th><th>Name</th><th>Email</th></tr>";
        if (users != null) {
            for (UserRegister user : users) {
                html += "<tr>";
                html += "<td>" + user.getId() + "</td>";
                html += "<td>" + StringEscapeUtils.escapeHtml(StringUtils.defaultString(user.getName())) + "</td>";
                html += "<td>" + StringEscapeUtils.escapeHtml(StringUtils.defaultString(user.getEmail())) + "</td>";
                html += "</tr>";
            }
        }
        html += "</table>";

        html += "<h3>Projects (" + (projects != null ? projects.size() : 0) + ")</h3>";
        html += "<table border='1'><tr><th>ID</th><th>Name</th><th>Start</th><th>End</th></tr>";
        if (projects != null) {
            for (ProjectRegister project : projects) {
                html += "<tr>";
                html += "<td>" + project.getProjectId() + "</td>";
                html += "<td>" + StringEscapeUtils.escapeHtml(StringUtils.defaultString(project.getProjectName())) + "</td>";
                html += "<td>" + (project.getStartDate() != null ? project.getStartDate().toString() : "N/A") + "</td>";
                html += "<td>" + (project.getEndDate() != null ? project.getEndDate().toString() : "N/A") + "</td>";
                html += "</tr>";
            }
        }
        html += "</table></body></html>";

        return html;
    }
}
