# pg-task-lease

<details name="readme-language">
<summary><picture><source media="(prefers-color-scheme: dark)" srcset="assets/lang-en-dark.svg"><img src="assets/lang-en-light.svg" width="42" height="26" alt="EN"></picture></summary>

**Version:** [0.3.0](docs/0.3.0/en.md) · [Versions](docs/index.md) · Java [17 / 21 / 25 / 27](docs/JAVA-SUPPORT.md)

Background reports, exports and notifications stored in PostgreSQL. Submit a task once, let independent workers process it, and recover unfinished work after a restart. Suitable when PostgreSQL already belongs to your application and a bounded task queue is enough.

## Get started in an existing Spring project

For Boot 4.1.2 with WebFlux, use the [starter](https://github.com/dlorian59/pg-task-lease-spring-boot-starter) below; it includes this core library and supplies the worker lifecycle. Java 17 is the minimum. Artifacts are not on Maven Central yet; [development](CONTRIBUTING.md) explains local preparation.

Maven (inside dependencies):
```xml
<dependency>
  <groupId>io.github.59alex</groupId>
  <artifactId>pg-task-lease-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>r2dbc-postgresql</artifactId>
  <scope>runtime</scope>
</dependency>
```
Gradle Kotlin DSL (your existing Boot BOM/plugin manages versions):
```kotlin
dependencies {
    implementation("io.github.59alex:pg-task-lease-spring-boot-starter:0.1.0")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    runtimeOnly("org.postgresql:r2dbc-postgresql")
}
```

Apply [the migration](src/main/resources/pg-task-lease.sql) through your application migration tool before enabling workers, then add application.yml:

```yaml
spring:
  r2dbc:
    url: ${TASKS_R2DBC_URL}
    username: ${TASKS_DB_USER}
    password: ${TASKS_DB_PASSWORD}
  sql:
    init:
      mode: never
pg-task-lease:
  worker:
    enabled: true
```

The [ready controller and service](https://github.com/dlorian59/pg-task-lease-spring-boot-starter/blob/main/README.md) accept PUT /reports/{id}?quarter=2026-Q4 and expose GET /reports/{id}. Copy the linked complete source into this structure beneath your current application scan package:

```text
src/main/java/example/
  ReportController.java
  ReportService.java
  ReportJobs.java
src/main/resources/application.yml
src/main/resources/db/migration/Vxxx__pg_task_lease.sql
```

The service injects PgTaskQueue and submits with one expression:

```java
queue.submit(TaskRequest.ready(id, "reports", "2026-Q4"));
```

The starter's [complete CSV handler](https://github.com/dlorian59/pg-task-lease-spring-boot-starter/blob/main/examples/report-worker/src/main/java/example/ReportJobs.java) uses @PgTaskHandler(value = "reports", blocking = true) on a normal void method. It writes one report keyed by the task UUID; no polling or heartbeat implementation is needed in the application. HTTP 202 means accepted submission, GET eventually returns DONE, and reports/&lt;id&gt;.csv contains the result. Authentication, ownership checks and real report data belong to your application. [Step-by-step explanation](https://github.com/dlorian59/pg-task-lease-spring-boot-starter/blob/main/docs/0.1.0/en.md).

## Core capabilities

1. **Submit with defaults:** `TaskRequest.ready(id, "reports", quarter)` → priority 0, five attempts, immediate availability. Reuse the ID across transport retries; submit returns false for an existing ID without replacing it. [Full source](docs/0.3.0/examples/ReportExample.java), [explanation](docs/0.3.0/en.md#2-submit-once-and-retry-submission-safely).
2. **Change just one choice:** `request.withDelay(Duration.ofMinutes(5)).withPriority(10)` → a new validated request. `.withMaxAttempts(2)` bounds claims. [Reference](docs/0.3.0/reference.en.md#ready-requests-and-immutable-overrides).
3. **Share work:** `queue.claim("reports", "worker-a", 4, Duration.ofSeconds(30))` → up to four distinct tasks owned by this attempt. [Parallel example](docs/0.3.0/en.md#3-claim-batches-with-parallel-workers).
4. **Acknowledge current work:** `queue.complete(lease)` → true only for current, unexpired ownership. `queue.heartbeat(lease, Duration.ofSeconds(30))` extends it. [Full example](docs/0.3.0/examples/Example.java), [ownership](docs/0.3.0/en.md#4-complete-only-with-current-ownership).
5. **Retry or recover:** `queue.fail(lease, Duration.ofSeconds(5), "Temporary failure")` releases a current attempt; `queue.recoverExpired("reports", 100, Duration.ofSeconds(5))` recovers abandoned work. Exhausted tasks become DEAD. [Failure walkthrough](docs/0.3.0/en.md#6-retry-exhaust-the-budget-and-recover-expiry).

## Without Spring

Use `io.github.59alex:pg-task-lease:0.3.0` plus an R2DBC PostgreSQL driver. [Core guide](docs/0.3.0/en.md) and [complete finite report](docs/0.3.0/reports.en.md) show client creation and explicit operations. Core has no background scheduler; your host owns polling, heartbeats, recovery and the connection pool. The starter is the shorter path when these responsibilities belong to Spring.

Processing attempts are at least once within the retry budget. External effects need an idempotency key; a lease protects queue state, not another system. Domain changes and queue acknowledgment are separate transactions. [Guarantees and failure boundaries](docs/0.3.0/reference.en.md).

[Full guide](docs/0.3.0/en.md) · [API reference](docs/0.3.0/reference.en.md) · [Development](CONTRIBUTING.md)

</details>

<details name="readme-language" open>
<summary><picture><source media="(prefers-color-scheme: dark)" srcset="assets/lang-ru-dark.svg"><img src="assets/lang-ru-light.svg" width="42" height="26" alt="RU"></picture></summary>

**Версия:** [0.3.0](docs/0.3.0/ru.md) · [Версии](docs/index.md) · Java [17 / 21 / 25 / 27](docs/JAVA-SUPPORT.md)

Фоновые отчёты, экспорты и уведомления с хранением в PostgreSQL. Отправьте задачу один раз, распределите обработку между работниками и восстановите незавершённую работу после перезапуска. Подходит приложению, у которого уже есть PostgreSQL и достаточно очереди с ограниченными попытками.

## Подключение к существующему Spring-проекту

Для Boot 4.1.2 с WebFlux используйте [стартер](https://github.com/dlorian59/pg-task-lease-spring-boot-starter) ниже: он включает core и управляет работником. Минимальная Java — 17. Артефактов пока нет в Maven Central; локальная подготовка описана в [документе разработки](CONTRIBUTING.md).

Maven (внутри dependencies):
```xml
<dependency>
  <groupId>io.github.59alex</groupId>
  <artifactId>pg-task-lease-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>r2dbc-postgresql</artifactId>
  <scope>runtime</scope>
</dependency>
```
Gradle Kotlin DSL (версиями управляет ваш Boot BOM/plugin):
```kotlin
dependencies {
    implementation("io.github.59alex:pg-task-lease-spring-boot-starter:0.1.0")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    runtimeOnly("org.postgresql:r2dbc-postgresql")
}
```

До включения worker примените [миграцию](src/main/resources/pg-task-lease.sql) через обычный инструмент приложения, затем добавьте application.yml:

```yaml
spring:
  r2dbc:
    url: ${TASKS_R2DBC_URL}
    username: ${TASKS_DB_USER}
    password: ${TASKS_DB_PASSWORD}
  sql:
    init:
      mode: never
pg-task-lease:
  worker:
    enabled: true
```

[Готовые контроллер и сервис](https://github.com/dlorian59/pg-task-lease-spring-boot-starter/blob/main/README.md) принимают PUT /reports/{id}?quarter=2026-Q4 и предоставляют GET /reports/{id}. Скопируйте полные исходники по ссылке в структуру под существующим пакетом сканирования приложения:

```text
src/main/java/example/
  ReportController.java
  ReportService.java
  ReportJobs.java
src/main/resources/application.yml
src/main/resources/db/migration/Vxxx__pg_task_lease.sql
```

Сервис получает PgTaskQueue через DI и отправляет одним выражением:

```java
queue.submit(TaskRequest.ready(id, "reports", "2026-Q4"));
```

[Полный CSV-обработчик](https://github.com/dlorian59/pg-task-lease-spring-boot-starter/blob/main/examples/report-worker/src/main/java/example/ReportJobs.java) стартера объявляет @PgTaskHandler(value = "reports", blocking = true) на обычном void-методе. Он пишет отчёт с ключом по UUID задачи; опрос и heartbeat в приложении реализовывать не нужно. HTTP 202 означает принятую отправку, GET впоследствии возвращает DONE, а reports/&lt;id&gt;.csv содержит результат. Аутентификация, проверка владельца и реальные данные — ответственность приложения. [Пошаговый разбор](https://github.com/dlorian59/pg-task-lease-spring-boot-starter/blob/main/docs/0.1.0/ru.md).

## Возможности core

1. **Отправить с defaults:** `TaskRequest.ready(id, "reports", quarter)` → priority 0, пять попыток, доступна сразу. Сохраняйте ID при сетевых повторах: submit возвращает false для существующего ID, не заменяя строку. [Полный код](docs/0.3.0/examples/ReportExample.java), [разбор](docs/0.3.0/ru.md#2-создать-задачу-и-безопасно-повторить-отправку).
2. **Изменить только нужный параметр:** `request.withDelay(Duration.ofMinutes(5)).withPriority(10)` → новый проверенный запрос. `.withMaxAttempts(2)` ограничивает захваты. [Справочник](docs/0.3.0/reference.ru.md#готовые-запросы-и-неизменяемые-копии).
3. **Разделить работу:** `queue.claim("reports", "worker-a", 4, Duration.ofSeconds(30))` → до четырёх разных задач с владением этой попытки. [Параллельный пример](docs/0.3.0/ru.md#3-забирать-задачи-несколькими-работниками).
4. **Подтвердить текущую работу:** `queue.complete(lease)` → true только для текущей непросроченной аренды. `queue.heartbeat(lease, Duration.ofSeconds(30))` продлевает её. [Полный пример](docs/0.3.0/examples/Example.java), [владение](docs/0.3.0/ru.md#4-завершать-только-с-действующей-арендой).
5. **Повторить или восстановить:** `queue.fail(lease, Duration.ofSeconds(5), "Temporary failure")` освобождает текущую попытку; `queue.recoverExpired("reports", 100, Duration.ofSeconds(5))` возвращает брошенные задачи. При исчерпании попыток — DEAD. [Разбор отказов](docs/0.3.0/ru.md#6-повторять-ограничивать-попытки-и-возвращать-просроченные-задачи).

## Без Spring

Используйте `io.github.59alex:pg-task-lease:0.3.0` и драйвер R2DBC PostgreSQL. [Руководство core](docs/0.3.0/ru.md) и [полный конечный отчёт](docs/0.3.0/reports.ru.md) показывают создание клиента и явные операции. У core нет фонового планировщика: приложение владеет опросом, heartbeat, восстановлением и пулом. Стартер сокращает код, когда этими обязанностями должен управлять Spring.

Попытки обработки — at-least-once в пределах бюджета. Внешним эффектам нужен ключ идемпотентности: аренда защищает очередь, а не другую систему. Доменные изменения и подтверждение очереди — разные транзакции. [Гарантии и отказы](docs/0.3.0/reference.ru.md).

[Полное руководство](docs/0.3.0/ru.md) · [Справочник API](docs/0.3.0/reference.ru.md) · [Разработка](CONTRIBUTING.md)

</details>

[MIT](LICENSE).
