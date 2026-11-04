# 0.3.0 — API and capability map

[Versions](../index.md) · [Guide](en.md) · [English](reference.en.md) · [Русский](reference.ru.md) · [README](../../README.md)

## Capability → code → limits

All operations belong to [PgTaskQueue](../../src/main/java/io/github/alex59/pgtasklease/PgTaskQueue.java). The [complete example](examples/Example.java) runs submission, claims, heartbeat, stale-lease rejection, retry and recovery; the [report example](examples/ReportExample.java) shows an idempotent external result.

| Public operation | Code to use | Result and boundary |
|---|---|---|
| Constructor | new PgTaskQueue(connectionFactory) | Requires a nonnull R2DBC factory. Pool lifecycle belongs to the caller. No migration, scheduler or handler is installed. |
| submit(TaskRequest) | queue.submit(request) | Mono<Boolean>: true inserts; false means that UUID already exists globally across all queues. Existing content is not updated or compared. |
| claim(queue, owner, limit, leaseTime) | queue.claim("reports", "worker-a", 4, Duration.ofSeconds(30)) | Mono<List<TaskLease>>: immutable bounded batch; empty when nothing is eligible. Atomic SKIP LOCKED, priority preference, not global FIFO or fairness. |
| heartbeat(lease, leaseTime) | queue.heartbeat(lease, Duration.ofSeconds(30)) | Mono<Boolean>: renews a current unexpired lease without shortening it. Does not mutate the acquisition snapshot. |
| complete(lease) | queue.complete(lease) | Mono<Boolean>: true transitions to DONE; false means no current ownership, including repeated completion. |
| fail(lease, retryDelay, error) | queue.fail(lease, Duration.ofSeconds(5), "report unavailable") | Mono<Boolean>: current lease becomes READY or DEAD when the claim budget is exhausted. Error text is stored verbatim. |
| recoverExpired(queue, limit, retryDelay) | queue.recoverExpired("reports", 100, Duration.ZERO) | Mono<Integer>: recovered row count. Expired leases become READY/DEAD; several recovery processes may coexist. Host must schedule it. |
| find(id) | queue.find(taskId) | Mono<TaskSnapshot>: current diagnostic snapshot, empty for unknown UUID. Does not confer ownership. |

The expressions above use an existing queue, request, lease and taskId; they are API fragments, not standalone programs. Use the linked examples for imports, connections, sequencing and assertions. The [practical guide](reports.en.md) covers compiling, running and expected CSV/output.

## Records and states

- [TaskRequest](../../src/main/java/io/github/alex59/pgtasklease/TaskRequest.java): id, queue, payload, priority, maxAttempts, delay. Choose and retain the UUID before retrying submission. Payload is opaque nonnull text; application serialization and size limits are your responsibility. Higher integer priority is preferred; maxAttempts is positive and counts claims, including claims lost to crashes.
- [TaskLease](../../src/main/java/io/github/alex59/pgtasklease/TaskLease.java): id, queue, payload, priority, attempt, maxAttempts, token, generation, owner, expiresAt. Token plus generation authorizes updates; owner is diagnostic. expiresAt is the original acquisition deadline, not a live heartbeat result. Treat the capability as confidential.
- [TaskSnapshot](../../src/main/java/io/github/alex59/pgtasklease/TaskSnapshot.java): id, queue, state, attempts, maxAttempts, generation, availableAt, expiresAt, owner, lastError. Lease deadline and owner are null outside LEASED; lastError may be null. Records are immutable snapshots, not managed entities.
- [TaskState](../../src/main/java/io/github/alex59/pgtasklease/TaskState.java): READY awaits availability/claim; LEASED has temporary ownership; DONE was acknowledged; DEAD exhausted attempts. No automatic DEAD replay API is provided.

## Validation and reactive execution

Queue names are nonblank, at most 120 Java characters; owners at most 200. Claim/recovery batches are 1..1000. Durations are whole milliseconds, at most 365 days: leaseTime is positive; submission delay and retryDelay may be zero. Failure text is nonnull, at most 2048 Java characters; remove secrets before storing it. Invalid arguments fail synchronously before SQL. Required nulls cause NullPointerException; invalid ranges cause IllegalArgumentException. Database/connection failures are emitted through the publisher.

Publishers are cold: nothing executes until subscription; each subscription obtains and closes one auto-commit connection. Repeated subscription repeats the operation. Only submit with the same retained UUID has the documented submission deduplication property. Await every result; boolean false is not a successful acknowledgment. Cancellation/timeout cannot prove that a database operation did not commit.

## Schema, retention and reliability

Apply [pg-task-lease.sql](../../src/main/resources/pg-task-lease.sql) explicitly before use. The SQL targets public.pg_task_lease; no configurable schema, automatic upgrade or domain-transaction joining is provided. Queue claims and fenced updates rely on PostgreSQL row locks, database wall-clock expiry and retained committed rows. Fenced updates recheck expiry after taking the lock.

At-least-once attempts require active workers and recovery, sufficient attempt budget and database durability. Handler effects may precede a lost acknowledgment; queue fencing does not undo or fence your external system. Make effects idempotent using the task UUID or a downstream fencing contract. Submission and domain writes need an application-owned transaction/outbox design if atomicity is required.

There is no cleanup API. Delete terminal rows only under an explicit retention policy that exceeds producer retry/deduplication requirements. Deletion removes UUID history and resets the retained-row generation sequence if that UUID is submitted again. Never delete READY/LEASED rows as routine cleanup. Account for payload/error sensitivity, database growth, indexes and vacuuming.

For managed polling, heartbeat, recovery and proxied Spring handlers, see the [starter use case](reports.en.md#reduce-the-lifecycle-code-with-spring-boot). The core intentionally contains no Spring/AOP registration mechanism.

[Guide](en.md) · [Practical example](reports.en.md) · [README](../../README.md)
