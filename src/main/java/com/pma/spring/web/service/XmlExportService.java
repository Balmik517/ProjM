package com.pma.spring.web.service;

import java.io.File;
import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.UserRegister;

/**
 * XML export/import service using JAXB (javax.xml.bind).
 * javax.xml.bind was removed from JDK 11 and moved to Jakarta namespace.
 * This is a classic legacy pattern for XML data exchange.
 */
@Service
public class XmlExportService {

    private static final Logger logger = Logger.getLogger(XmlExportService.class);

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private ProjectServiceImpl projectService;

    /**
     * Export all users to XML string using JAXB marshalling.
     */
    public String exportUsersToXml() {
        try {
            List<UserRegister> users = userService.findAll();
            if (users == null || users.isEmpty()) {
                return "<users/>";
            }

            UserListWrapper wrapper = new UserListWrapper();
            wrapper.setUsers(users);

            JAXBContext context = JAXBContext.newInstance(UserListWrapper.class, UserRegister.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

            StringWriter writer = new StringWriter();
            marshaller.marshal(wrapper, writer);

            String xml = writer.toString();
            logger.info("Exported " + users.size() + " users to XML");
            return xml;
        } catch (JAXBException e) {
            logger.error("JAXB marshalling failed for users", e);
            throw new RuntimeException("XML export failed", e);
        }
    }

    /**
     * Export all projects to XML string using JAXB.
     */
    public String exportProjectsToXml() {
        try {
            List<ProjectRegister> projects = projectService.findAll();
            if (projects == null || projects.isEmpty()) {
                return "<projects/>";
            }

            ProjectListWrapper wrapper = new ProjectListWrapper();
            wrapper.setProjects(projects);

            JAXBContext context = JAXBContext.newInstance(ProjectListWrapper.class, ProjectRegister.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            StringWriter writer = new StringWriter();
            marshaller.marshal(wrapper, writer);

            logger.info("Exported " + projects.size() + " projects to XML");
            return writer.toString();
        } catch (JAXBException e) {
            logger.error("JAXB marshalling failed for projects", e);
            throw new RuntimeException("XML export failed", e);
        }
    }

    /**
     * Import users from an XML file using JAXB unmarshalling.
     */
    public int importUsersFromXml(String filePath) {
        try {
            JAXBContext context = JAXBContext.newInstance(UserListWrapper.class, UserRegister.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            File file = new File(filePath);
            UserListWrapper wrapper = (UserListWrapper) unmarshaller.unmarshal(file);

            int count = 0;
            if (wrapper.getUsers() != null) {
                for (UserRegister user : wrapper.getUsers()) {
                    userService.save(user);
                    count++;
                }
            }
            logger.info("Imported " + count + " users from XML: " + filePath);
            return count;
        } catch (JAXBException e) {
            logger.error("JAXB unmarshalling failed", e);
            throw new RuntimeException("XML import failed", e);
        }
    }

    /**
     * Generate system report in XML format.
     */
    public String generateSystemReport() {
        try {
            SystemReport report = new SystemReport();
            report.setApplicationName("Project Management App");
            report.setVersion("1.0.0-LEGACY");
            report.setJavaVersion(System.getProperty("java.version"));
            report.setOsName(System.getProperty("os.name"));

            List<UserRegister> users = userService.findAll();
            report.setTotalUsers(users != null ? users.size() : 0);

            List<ProjectRegister> projects = projectService.findAll();
            report.setTotalProjects(projects != null ? projects.size() : 0);

            report.setTimestamp(System.currentTimeMillis());

            JAXBContext context = JAXBContext.newInstance(SystemReport.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            StringWriter writer = new StringWriter();
            marshaller.marshal(report, writer);
            return writer.toString();
        } catch (JAXBException e) {
            logger.error("Failed to generate system report", e);
            throw new RuntimeException("Report generation failed", e);
        }
    }

    // ---- JAXB wrapper classes ----

    @XmlRootElement(name = "users")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class UserListWrapper {
        @XmlElement(name = "user")
        private List<UserRegister> users;

        public List<UserRegister> getUsers() { return users; }
        public void setUsers(List<UserRegister> users) { this.users = users; }
    }

    @XmlRootElement(name = "projects")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProjectListWrapper {
        @XmlElement(name = "project")
        private List<ProjectRegister> projects;

        public List<ProjectRegister> getProjects() { return projects; }
        public void setProjects(List<ProjectRegister> projects) { this.projects = projects; }
    }

    @XmlRootElement(name = "systemReport")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SystemReport {
        private String applicationName;
        private String version;
        private String javaVersion;
        private String osName;
        private int totalUsers;
        private int totalProjects;
        private long timestamp;

        public String getApplicationName() { return applicationName; }
        public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getJavaVersion() { return javaVersion; }
        public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }
        public String getOsName() { return osName; }
        public void setOsName(String osName) { this.osName = osName; }
        public int getTotalUsers() { return totalUsers; }
        public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }
        public int getTotalProjects() { return totalProjects; }
        public void setTotalProjects(int totalProjects) { this.totalProjects = totalProjects; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
