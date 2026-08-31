package com.pma.spring.web.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Loads the whole monolith context for a test.
 *
 * The single property below is a JDK compatibility workaround, not an
 * application setting: Spring Boot's Groovy template auto-configuration is
 * pulled in by the legacy {@code groovy-all} dependency and fails to initialise
 * on JDK 17+ because it reflects into a generated proxy module. Nothing in the
 * application renders Groovy templates, and the main
 * {@code application.properties} is deliberately left untouched so the Java 8
 * baseline behaviour is preserved. When running the application itself on a
 * modern JDK, pass the same exclusion plus the {@code --add-opens} flags listed
 * in the README.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.groovy.template."
                + "GroovyTemplateAutoConfiguration" })
public @interface MonolithTest {
}
