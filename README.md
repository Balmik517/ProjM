# ProjM

A Java 8 / Spring Boot 2.5 / Spring MVC project management application, packaged
as a single deployable monolith.

## Purpose

ProjM is a synthetic Java 8 Spring MVC monolith created as a
benchmark for architecture and decomposition analysis.

The application intentionally contains a mixture of:

- cohesive modules
- cross-domain dependencies
- shared persistence
- transaction coupling
- circular dependencies
- scheduled processing
- legacy utilities
- high-coupling reporting
- potential service boundaries

All data, names, addresses, hosts and credentials in this repository are
fabricated. There is no real customer, client or company information here, and
the repository is safe to share.

## Reading this repository

The package layout is *not* the architecture. Every service lives in
`com.pma.spring.web.service`, every entity in `com.pma.spring.web.entity`, and
every repository in `com.pma.spring.web.repository`, following the conventions
the application already used. Conceptual domains have to be inferred from what
the code actually does - which classes call which, which components read and
write which tables, and where transaction boundaries fall - rather than from
package names.

The conceptual domains present in the code are:

```
Identity / User Management
Project Management
Change Requests
Task Management
Billing
Payments
Notifications
Audit
Reporting
External Integration
```

No file in this repository states which of these should become a service. That
is the question the analysis is meant to answer.

## Domain tables

Alongside the original `user_register`, `project_register`, `change_request` and
`legacy_batch_task` tables, the schema contains:

| Table | Purpose |
| --- | --- |
| `project_members` | membership of a user in a project |
| `project_tasks` | work items, effort estimates and actuals |
| `invoices` | billing documents raised against a project |
| `payments` | settlements against an invoice |
| `notifications` | outbound message queue |
| `audit_events` | append-only audit trail |
| `report_snapshots` | materialised reporting output |
| `integration_requests` | outbox for calls leaving the monolith |

Schema creation is handled by Hibernate (`spring.jpa.hibernate.ddl-auto=update`)
against the embedded H2 database configured in `application.properties`. The
original Oracle settings are still present, commented out.

## Running

```
mvn clean package
java -jar target/SpringBootSampleApp-1-0.0.1-SNAPSHOT.jar
```

The application listens on port 8086. Tests are skipped by default via the
`maven.test.skip` property that the build already sets; run them with:

```
mvn test -Dmaven.test.skip=false
```

### Running on a JDK newer than 8

The Java 8 baseline is deliberate and unchanged. Several of the legacy
dependencies (XStream, Groovy, `sun.misc.Unsafe`) reflect into JDK internals, so
on a modern JDK the application needs the module flags below. The build already
declares them for the test runner; pass them yourself when launching the jar.

```
java \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.util=ALL-UNNAMED \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens java.base/java.text=ALL-UNNAMED \
  --add-opens java.base/java.io=ALL-UNNAMED \
  --add-opens java.base/java.math=ALL-UNNAMED \
  --add-opens java.base/java.net=ALL-UNNAMED \
  --add-opens java.base/java.security=ALL-UNNAMED \
  --add-opens java.desktop/java.awt.font=ALL-UNNAMED \
  -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration \
  -jar target/SpringBootSampleApp-1-0.0.1-SNAPSHOT.jar
```

None of this is required on Java 8.

## HTTP surface

The original JSP screens (`/home`, `/registerForm`, `/loginForm`, `/projectList`
and friends) are unchanged. The newer REST endpoints are:

```
GET    /projects/{id}/members
POST   /projects/{id}/members
DELETE /projects/{id}/members/{userId}
GET    /projects/{id}/members/directory
GET    /projects/{id}/status

GET    /projects/{id}/tasks
POST   /projects/{id}/tasks
GET    /projects/{id}/tasks/effort
GET    /projects/tasks/{taskId}
PUT    /projects/tasks/{taskId}/status
PUT    /projects/tasks/{taskId}/work
PUT    /projects/tasks/{taskId}/assignee

GET    /invoices
POST   /invoices
GET    /invoices/{id}
POST   /invoices/{id}/settle
POST   /invoices/{id}/cancel
GET    /invoices/overview
GET    /invoices/project/{projectId}/view
GET    /invoices/project/{projectId}/raw

GET    /payments
POST   /payments
GET    /payments/{id}
POST   /payments/write-off
POST   /payments/{id}/fail
GET    /payments/stats

GET    /notifications
POST   /notifications
POST   /notifications/{id}/send
POST   /notifications/flush
GET    /notifications/stats
GET    /notifications/digest/{userId}

GET    /change-requests
POST   /change-requests
GET    /change-requests/{id}
POST   /change-requests/{id}/approve
POST   /change-requests/{id}/reject
POST   /change-requests/{id}/implement
GET    /change-requests/summary/{projectId}

GET    /reports/projects/{id}
GET    /reports/projects/{id}/snapshots
POST   /reports/projects/{id}/billing-snapshot
GET    /reports/snapshots
GET    /reports/billing
GET    /reports/operational
GET    /reports/workload
GET    /reports/changes
GET    /reports/legacy/users.csv

GET    /integrations
POST   /integrations
GET    /integrations/{id}
POST   /integrations/{id}/dispatch
POST   /integrations/{id}/retry
GET    /integrations/stats

POST   /operations/projects
PUT    /operations/projects/{id}
POST   /operations/projects/{id}/members
POST   /operations/projects/{id}/tasks
POST   /operations/projects/{id}/invoices
POST   /operations/projects/{id}/reports
POST   /operations/projects/{id}/integrations
POST   /operations/projects/{id}/reprioritise
POST   /operations/projects/{id}/complete
GET    /operations/projects/{id}/dashboard
POST   /operations/payments
POST   /operations/notifications
GET    /operations/billing/overview
GET    /operations/reports/legacy-html
```

## Signals available to an analyser

Everything an architecture or decomposition tool needs is discoverable from the
source: controller-to-service and service-to-repository wiring, `@Autowired`
field injection graphs, JPA entity and table mappings, derived and `@Query`
repository methods, native SQL, raw JDBC executed through a shared helper,
`@Transactional` boundaries and propagation modes, `@Scheduled` and Quartz
triggers, static service-locator lookups, and per-method complexity.

Two access paths deliberately do not appear in the JPA repository graph: the
shared JDBC helper in `com.pma.spring.web.util`, and the legacy DAO that predates
it. A dependency graph built only from repository injection will understate how
many components read a given table.

## Tests

`mvn test -Dmaven.test.skip=false` runs 63 tests covering the new repositories,
the domain services, the cross-domain and wide transactional workflows, the
scheduled jobs, and the HTTP endpoints, plus a regression check that the original
endpoints still respond.

The tests share one in-memory database across test classes, so fixtures use
unique synthetic values and assertions are written relative to the rows each test
creates.
