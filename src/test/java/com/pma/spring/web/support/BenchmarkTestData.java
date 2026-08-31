package com.pma.spring.web.support;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import com.pma.spring.web.entity.ProjectRegister;
import com.pma.spring.web.entity.UserRegister;
import com.pma.spring.web.repository.ProjectRepository;
import com.pma.spring.web.repository.UserRepository;

/**
 * Builds synthetic users and projects for the tests.
 *
 * Every value here is fabricated. The shared in-memory database is not reset
 * between test classes, so each fixture gets a unique suffix and assertions are
 * written relative to the rows the test itself created.
 */
public final class BenchmarkTestData {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private BenchmarkTestData() {
    }

    public static UserRegister newUser(UserRepository userRepository, String label) {
        int seq = SEQUENCE.incrementAndGet();
        UserRegister user = new UserRegister();
        user.setName(label + "-" + seq);
        user.setEmail(label + seq + "@example.com");
        user.setPassword("pw" + seq);
        user.setPhoneNumber("5550" + String.format("%05d", seq % 100000));
        user.setAddress("1 Sample Street");
        user.setDob(new Date());
        return userRepository.save(user);
    }

    public static ProjectRegister newProject(ProjectRepository projectRepository, String label) {
        int seq = SEQUENCE.incrementAndGet();
        ProjectRegister project = new ProjectRegister();
        project.setProjectName(label + "-" + seq);
        project.setStartDate(new Date());
        project.setEndDate(new Date());
        project.setStatus("ACTIVE");
        return projectRepository.save(project);
    }
}
