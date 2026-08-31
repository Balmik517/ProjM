package com.cs.spring.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// The exclusion below is a JDK compatibility workaround, not an application
// setting: Spring Boot's Groovy template auto-configuration is pulled in by the
// legacy groovy-all dependency and fails to initialise on JDK 17+ because it
// reflects into a generated proxy module. Nothing in the application renders
// Groovy templates. src/main/resources/application.properties is deliberately
// left untouched so the Java 8 baseline behaviour is preserved.
@SpringBootTest(classes = com.pma.spring.web.Project.class,
	properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration")
class BootDemo1ApplicationTests {
	@Test
	void contextLoads() {
	}
}
