package com.pma.spring.web.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.UserRepository;
import com.pma.spring.web.util.LegacyUtils;

/**
 * Grab-bag "common" component every other component reaches for.
 *
 * It is a shared technical dependency rather than a business capability, but
 * unlike {@link LegacyUtils} it also reads the identity and project tables,
 * so extracting it would drag two domains along with it.
 */
@Service
public class CommonService {

    private static final Logger logger = Logger.getLogger(CommonService.class);

    /** Synthetic default used when no rate is configured for a project. */
    public static final double DEFAULT_HOURLY_RATE = 75.0;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    /**
     * Resolve a display label for a user id without failing when the row is
     * missing - the legacy screens rely on this being lenient.
     */
    public String resolveUserLabel(int userId) {
        Optional<UserRegister> optional = userRepository.findById(userId);
        if (!optional.isPresent()) {
            return "user#" + userId;
        }
        UserRegister user = optional.get();
        return StringUtils.defaultIfBlank(user.getName(), "user#" + userId);
    }

    public String resolveUserEmail(int userId) {
        Optional<UserRegister> optional = userRepository.findById(userId);
        if (!optional.isPresent()) {
            return "unknown@example.com";
        }
        return StringUtils.defaultIfBlank(optional.get().getEmail(), "unknown@example.com");
    }

    public String resolveProjectLabel(int projectId) {
        Optional<ProjectRegister> optional = projectRepository.findById(projectId);
        if (!optional.isPresent()) {
            return "project#" + projectId;
        }
        return StringUtils.defaultIfBlank(optional.get().getProjectName(), "project#" + projectId);
    }

    public boolean projectExists(int projectId) {
        return projectRepository.existsById(projectId);
    }

    public boolean userExists(int userId) {
        return userRepository.existsById(userId);
    }

    /**
     * Hourly rate used by billing and reporting. In a real system this would
     * come from a contract table; here it is derived from the project id so the
     * benchmark stays fully synthetic.
     */
    public double resolveHourlyRate(int projectId) {
        Optional<ProjectRegister> optional = projectRepository.findById(projectId);
        if (!optional.isPresent()) {
            return DEFAULT_HOURLY_RATE;
        }
        return DEFAULT_HOURLY_RATE + (projectId % 5) * 5.0;
    }

    /**
     * The "current actor" in this monolith is whatever the caller says it is.
     */
    public int resolveActorId(Integer suppliedActorId) {
        if (suppliedActorId == null || suppliedActorId.intValue() <= 0) {
            return 0;
        }
        return suppliedActorId.intValue();
    }

    public Map<String, Object> describeEnvironment() {
        Map<String, Object> info = new HashMap<String, Object>();
        info.put("appMode", "monolith");
        info.put("timestamp", LegacyUtils.formatTimestamp(new Date()));
        info.put("totalUsers", Long.valueOf(userRepository.count()));
        info.put("totalProjects", Long.valueOf(projectRepository.count()));
        info.put("domainTouches", LegacyUtils.getDomainTouchCounters());
        return info;
    }

    public List<String> recentDomainActivity(int size) {
        logger.debug("Returning recent domain activity, size=" + size);
        return LegacyUtils.getRecentTouches(size);
    }
}
