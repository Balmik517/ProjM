package com.pma.spring.web.config;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pma.spring.web.scheduler.ReportGenerationJob;

/**
 * Quartz Scheduler configuration using old version 2.2.1.
 * Mixes Quartz standalone scheduling with Spring @Scheduled.
 * Uses javax.annotation @PostConstruct (removed from JDK 11).
 */
@Configuration
public class QuartzConfig {

    private static final Logger logger = Logger.getLogger(QuartzConfig.class);

    @PostConstruct
    public void init() {
        logger.info("QuartzConfig initialized - Quartz 2.2.1 (legacy)");
    }

    @Bean
    public Scheduler quartzScheduler() throws SchedulerException {
        SchedulerFactory factory = new StdSchedulerFactory();
        Scheduler scheduler = factory.getScheduler();

        // Schedule report generation job
        JobDetail reportJob = JobBuilder.newJob(ReportGenerationJob.class)
                .withIdentity("reportGeneration", "reports")
                .withDescription("Generate periodic system reports")
                .storeDurably()
                .build();

        Trigger reportTrigger = TriggerBuilder.newTrigger()
                .withIdentity("reportTrigger", "reports")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 6 * * ?")) // 6 AM daily
                .build();

        if (!scheduler.checkExists(reportJob.getKey())) {
            scheduler.scheduleJob(reportJob, reportTrigger);
        }

        scheduler.start();
        logger.info("Quartz Scheduler started with " + scheduler.getMetaData().getThreadPoolSize() + " threads");

        return scheduler;
    }
}
