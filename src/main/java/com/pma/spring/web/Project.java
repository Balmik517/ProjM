package com.pma.spring.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Application entry point.
 *
 * The original build only ever had one base package, so the default
 * component/entity/repository scan was implicit. Newer feature work lives in
 * sibling packages under {@code com.pma.spring} (billing, task, notification,
 * audit, reporting, integration) that group by conceptual domain rather than
 * by technical layer, so scanning is now explicit and covers both the legacy
 * {@code com.pma.spring.web} tree and the new domain packages.
 */
@SpringBootApplication
@SpringBootConfiguration
@ComponentScan(basePackages = "com.pma.spring")
@EntityScan(basePackages = "com.pma.spring")
@EnableJpaRepositories(basePackages = "com.pma.spring")
public class Project {

	public static void main(String[] args) {
		SpringApplication.run(Project.class, args);
		//System.out.println("This is Susmita");
	}

}
