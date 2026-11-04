# Background report: core API and Spring Boot integration

[Versions](../index.md) · [Core guide](en.md) · [English](reports.en.md) · [Русский](reports.ru.md) · [README](../../README.md)

A report request must survive a process restart and must not create a second task when the producer retries. pg-task-lease records a stable UUID and lets a worker temporarily own that row. A deterministic output keyed by that UUID makes repeated file generation idempotent; the queue alone does not make external effects exactly once.

## Run the plain Java case

Use [ReportExample.java](examples/ReportExample.java) with the existing [pom.xml](examples/pom.xml) and [schema.sql](examples/schema.sql). Follow the [database setup](en.md#8-run-the-example-and-diagnose-failures) using a fresh database and install core 0.3.0 with publishToMavenLocal first. From the example directory, with TASKS_R2DBC_URL set:

```bash
mvn -q compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
TASK_ID=7a62d0c1-22a8-4936-a00e-99918e3d2d32
java -cp "target/classes:$(cat target/classpath.txt)" ReportExample "$TASK_ID" 2026-Q4
java -cp "target/classes:$(cat target/classpath.txt)" ReportExample "$TASK_ID" 2026-Q4
```

Expected: the first run prints REPORT_DONE and writes `reports/<task-id>.csv`; the second prints `ALREADY_SUBMITTED state=DONE`. Choose a new UUID for a genuinely new request. The file contains:

```csv
quarter,invoices,total
2026-Q4,3,450.00
```

The sample explicitly submits, claims, writes through a temporary file and atomic rename, then acknowledges and checks DONE. File I/O errors call fail; ambiguous acknowledgment errors are not reclassified as file generation failures. Sample totals are labelled in source: replace them with your application query.

This is one finite attempt. A duplicate submission in READY/LEASED/DEAD is reported, not automatically executed. A production core-only host supplies polling, heartbeat, recovery, cancellation and concurrency limits. Long reports need renewal before lease expiry. Completion can fail after the file is already written, so the file key/content must remain safe to repeat.

## Reduce the lifecycle code with Spring Boot

The separate [pg-task-lease-spring-boot-starter](https://github.com/59Alex/pg-task-lease-spring-boot-starter) adds a PgTaskQueue bean from Boot's ConnectionFactory and an opt-in managed worker. Its version 0.1.0 report application uses the same UUID-based CSV use case, but the host only registers a TaskHandlerRegistration bean and enables `pg-task-lease.worker.enabled=true`.

The starter handles bounded polling, awaited handlers, heartbeat, expired-lease recovery, timeouts, retries and shutdown cancellation. Your own beans override defaults. Database migration remains explicit. There is no business annotation scanner or automatic transaction with domain writes.

The integration checkout is currently local; the GitHub destination is pending publication. With adjacent local checkouts, open `../pg-task-lease-spring-boot-starter/README.md`, then its `docs/0.1.0/en.md` and `examples/report-worker/`. After installing core into Maven Local, install the starter with `./gradlew build publishToMavenLocal` and run `examples/report-worker/run-demo.sh` from that checkout.

[README](../../README.md) · [Core API](en.md) · [Versions](../index.md)
