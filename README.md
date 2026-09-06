# ProjM

A Java 8 / Spring Boot 2.5 project management application, packaged as a
single deployable monolith, deliberately built as a **benchmark for
architecture and decomposition analysis** rather than as production software.

## What this project actually is

ProjM looks like an ordinary internal PMO tool: register users, create
projects, assign tasks, raise invoices, take payments, send notifications,
handle change requests, run reports, and talk to a couple of outside systems.
None of that data is real - every name, address, host and credential in the
repository is fabricated, and the whole thing is safe to share.

What it's actually *for* is different: it's a synthetic codebase for testing
tools that analyse a monolith and recommend how (or whether) to split it into
services - decomposition advisors, dependency-graph visualisers, coupling
metrics, that kind of thing. To be useful for that, the code has to look and
behave like software that grew organically over a few years, not like a
clean, hand-designed reference architecture. So it intentionally contains:

- cohesive modules that would extract cleanly
- cross-domain dependencies that would not
- shared persistence (several components reading or writing the same table)
- broad, multi-domain transactions
- circular dependencies
- scheduled/background processing with its own coupling
- legacy utilities with unusually high fan-in
- a reporting layer that is deliberately the most coupled thing in the app
- a handful of components that look extractable but secretly aren't

No file in the repository says which parts *should* become a service. That
judgement call is the question the codebase exists to let a tool (or a
person) answer, using nothing but the code, the schema, and the Git history.

## Tech stack

| Layer | Choice |
| --- | --- |
| Language | Java 8 (baseline is deliberate - see [Running on a JDK newer than 8](#running-on-a-jdk-newer-than-8)) |
| Framework | Spring Boot 2.5.1, Spring MVC, Spring Data JPA |
| ORM | Hibernate (via Spring Data JPA), `ddl-auto=update` |
| Database | H2 in-memory (default) - original Oracle 8i config left in place, commented out |
| Views | JSP (original screens) + JSON REST controllers (everything added since) |
| Scheduling | Spring `@Scheduled` jobs, plus a Quartz configuration |
| Build | Maven (`spring-boot-starter-parent`) |
| Logging | Log4j 1.x |
| Notable legacy/vulnerable deps kept on purpose | old `commons-collections`, `commons-lang` 2.x, `commons-beanutils`, XStream-style serialization helpers, `sun.misc.Unsafe` access, a legacy SOAP gateway, an old Apache HttpClient |

Those legacy and vulnerable dependencies are there deliberately, as part of
the "this is what a real aged monolith looks like" brief - not oversights.

## Package layout

The codebase currently mixes two layout styles on purpose, because that
mismatch is itself part of the benchmark.

**`com.pma.spring.web`** - the original application. Everything here is
organised by *technical layer*, not by business domain: every controller
lives in `com.pma.spring.web.controller`, every service in
`com.pma.spring.web.service`, every entity in `com.pma.spring.web.entity`,
every repository in `com.pma.spring.web.repository`, regardless of which
conceptual domain (billing, tasks, notifications, ...) it belongs to. There
is also a `com.pma.spring.web.legacy.*` tree of intentionally risky modules
(insecure crypto, unsafe native access, classloader leaks, unsafe
deserialization, a legacy SOAP gateway) and a `com.pma.spring.web.util`
package with a couple of very high-fan-in static helpers.

**`com.pma.spring.{task,billing,notification,audit,reporting,integration}`**
- newer feature work, added on top of the original app as domain-oriented
packages instead of being folded into `com.pma.spring.web`. Each of these
groups controller/service/repository/entity/dto together *by domain*, the
opposite convention from the original tree. Critically, these packages are
not sealed off from the original code or from each other: several of them
read (and in one case, delete from) tables that `com.pma.spring.web`
components still own, and a couple of small utilities are already shared
across package boundaries. `com.pma.spring.task` and
`com.pma.spring.integration` depend on each other in both directions - a
genuine package-level cycle that Java happily compiles. The Git history
shows this coupling being introduced one deliberate, focused commit at a
time rather than all at once - see
[Reading the Git history](#reading-the-git-history) below.

**`com.pma.spring.workflow`** - a single service
(`ProjectCompletionWorkflowService`) that completes a project in one
`@Transactional` method spanning seven packages: project, task, audit,
notification, billing, reporting and integration. It exists alongside
`ProjectDomainService.closeOutProject` in the original code - a second,
independent way to "complete a project" that the original method knows
nothing about, deliberately left as a consistency risk rather than resolved.

The package a class lives in is a hint, not the answer. Conceptual domains
have to be inferred from what the code actually does: which classes call
which, which components read and write which tables, and where transaction
boundaries fall.

### Conceptual domains present in the code

```
Identity / User Management
Project Management
Change Requests
Task Management         (project_tasks, task_comments, task_attachments)
Billing                 (invoices, credit_notes, tax_rates)
Payments
Notifications           (notifications, notification_preferences)
Audit                   (audit_events, audit_retention_policies)
Reporting               (report_snapshots, executive summaries, CSV export)
External Integration    (integration_requests, webhook_subscriptions)
```

## Database schema

Schema creation is handled entirely by Hibernate (`spring.jpa.hibernate.ddl-auto=update`)
against the embedded H2 database configured in `application.properties`. The
original Oracle settings are still present, commented out, for reference.

**Original tables:** `user_register`, `project_register`, `change_request`,
`legacy_batch_task`.

**Added since, still in `com.pma.spring.web`:**

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

**Added by the new domain-oriented packages:**

| Table | Owning package | Purpose |
| --- | --- | --- |
| `task_comments` | `com.pma.spring.task` | comments on a project task |
| `task_attachments` | `com.pma.spring.task` | attachment metadata for a task |
| `credit_notes` | `com.pma.spring.billing` | credit notes issued against an invoice |
| `tax_rates` | `com.pma.spring.billing.tax` | configurable per-region tax percentage |
| `notification_preferences` | `com.pma.spring.notification` | per-user, per-channel opt-in/opt-out |
| `audit_retention_policies` | `com.pma.spring.audit` | how long audit rows are kept, per entity type |
| `webhook_subscriptions` | `com.pma.spring.integration` | external systems subscribed to an event type |

## Running

```
mvn clean package
java -jar target/SpringBootSampleApp-1-0.0.1-SNAPSHOT.jar
```

The application listens on port `8086`. The H2 console is available at
`/h2-console` (JDBC URL `jdbc:h2:mem:legacydb`, user `sa`, no password).
Tests are skipped by default via the `maven.test.skip` property the build
sets; run them explicitly with:

```
mvn test -Dmaven.test.skip=false
```

### Running on a JDK newer than 8

The Java 8 baseline is deliberate and unchanged. Several of the legacy
dependencies (XStream, Groovy, `sun.misc.Unsafe`) reflect into JDK internals,
so on a modern JDK the application needs the module flags below. The build
already declares them for the test runner; pass them yourself when launching
the jar.

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

The original JSP screens (`/home`, `/registerForm`, `/loginForm`,
`/projectList` and friends) are unchanged. The REST endpoints, grouped by
where they live:

**`com.pma.spring.web` (original + earlier feature waves)**

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

**`com.pma.spring.task`**

```
GET    /tasks/{taskId}/comments
POST   /tasks/{taskId}/comments
GET    /tasks/{taskId}/attachments
POST   /tasks/{taskId}/attachments
```

**`com.pma.spring.billing` and `com.pma.spring.billing.tax`**

```
POST   /billing/credit-notes
GET    /billing/credit-notes/invoice/{invoiceId}
GET    /billing/credit-notes/invoice/{invoiceId}/outstanding
POST   /billing/tax-rates
GET    /billing/tax-rates/calculate?amount={amount}&region={region}
```

**`com.pma.spring.notification`**

```
POST   /notifications/preferences
GET    /notifications/preferences/user/{userId}
```

**`com.pma.spring.audit`**

```
GET    /audit/retention/policies
POST   /audit/retention/policies
POST   /audit/retention/purge?entityType={entityType}
```

**`com.pma.spring.reporting`**

```
GET    /reporting/executive-summary/{projectId}
GET    /reporting/export/{snapshotId}/csv
```

**`com.pma.spring.integration`**

```
POST   /integration/webhooks
GET    /integration/webhooks?eventType={eventType}
```

**`com.pma.spring.workflow`**

```
POST   /workflow/projects/{projectId}/complete-full?actorId={actorId}
```

## Scheduled jobs

Original: `BillingScheduler` (invoice sweep, reconciliation, write-offs),
`DataCleanupScheduler` (also the single `@EnableScheduling` source for the
whole app), `IntegrationDispatchScheduler`, `ReportGenerationJob`.

Added since: `NotificationDigestScheduler`
(`com.pma.spring.notification.scheduler`, builds a per-user digest by
reading both `notifications` and `audit_events`) and
`WebhookDispatchScheduler` (`com.pma.spring.integration.scheduler`, fans
pending notifications out to subscribed webhooks and writes an audit row per
dispatch).

## Reading the Git history

The package layout is not the only place coupling shows up - the commit
history is part of the benchmark too. Recent history introduces the new
domain packages one feature at a time, each as a focused, believably-scoped
commit (`feat: add task comment collaboration`, `feat: add invoice credit
notes`, and so on) rather than one large dump. A few things worth noticing
while reading it:

- **`com.pma.spring.reporting`** was built to be the most coupled thing added
  so far, on purpose - a single read spans five tables across two packages.
- A couple of components quietly start reading (and in one case, deleting
  from) tables that the original code's own documentation claims it owns
  exclusively - see `AuditRetentionService`, which reads and purges
  `audit_events` directly, next to the original `AuditService`.
- A small shared utility (`WebhookPayloadUtil`) is called from two unrelated
  packages, a miniature version of the high-fan-in shape `LegacyUtils`
  already has at the scale of the whole original application.

None of this is randomly generated. Every commit is a plausible, focused
engineering change (`feat`, `fix`, `refactor`, `chore`); nothing exists
purely to pad the commit count, because that would destroy the
Git-co-change signal a decomposition advisor might otherwise pick up on.

## Signals available to an analyser

Everything an architecture or decomposition tool needs is discoverable from
the source: controller-to-service and service-to-repository wiring,
`@Autowired` field injection graphs, JPA entity and table mappings, derived
and `@Query` repository methods, native SQL, raw JDBC executed through a
shared helper, `@Transactional` boundaries and propagation modes,
`@Scheduled` and Quartz triggers, static service-locator lookups,
per-method complexity, and Git co-change across the commit history.

Two access paths deliberately do not appear in the JPA repository graph: the
shared JDBC helper in `com.pma.spring.web.util`, and the legacy DAO that
predates it. A dependency graph built only from repository injection will
understate how many components read a given table.

## Tests

`mvn test -Dmaven.test.skip=false` runs the existing test suite covering the
repositories, domain services, cross-domain and wide transactional
workflows, scheduled jobs, and HTTP endpoints, plus a regression check that
the original endpoints still respond. The newer domain packages
(`task`, `billing`, `notification`, `audit`, `reporting`, `integration`) and
`com.pma.spring.workflow` each have their own test classes now too, using a
separate `DomainBenchmarkTestData` helper (kept apart from the original
`BenchmarkTestData`) for task/invoice/member fixtures. Coverage includes the
task/notification and task/integration cross-package edges, the audit
retention purge, the executive summary's cross-domain aggregation, the
webhook dispatch sent-status fix, and the full seven-package project
completion transaction, including its outstanding-balance and
already-completed edge cases.

The tests share one in-memory database across test classes, so fixtures use
unique synthetic values and assertions are written relative to the rows each
test creates.
