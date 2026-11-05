# Development / Разработка

[README](README.md) · [Guides / Руководства](docs/index.md)

## English

Artifacts are currently prepared from source in Maven Local, not downloaded from Maven Central. Use JDK 17+ and the checked-in Gradle wrapper. PostgreSQL tests require a fresh disposable database ending in _test and refuse an existing queue table.

```bash
./scripts/test-isolated.sh -PtestJavaVersion=17
./gradlew publishToMavenLocal
```

The first command starts its own PostgreSQL 17 container, runs the build and removes only that container. Docker must be healthy. Alternatively supply PG_TASK_TEST_URL, PG_TASK_TEST_USER and PG_TASK_TEST_PASSWORD for your own fresh disposable database and run ./gradlew build. Do not point this suite at application data. Source and Javadoc are included in local publication.

Java 17 is the compilation baseline; use -PtestJavaVersion=21, 25 or 27 with an installed matching JDK to test another runtime. [Runtime details](docs/JAVA-SUPPORT.md). This does not run a compatibility matrix automatically.

The [plain Java report consumer](docs/0.3.0/reports.en.md) uses Maven; the [Boot starter](https://github.com/dlorian59/pg-task-lease-spring-boot-starter/blob/main/CONTRIBUTING.md) shows Maven and Gradle consumer builds plus actual HTTP verification.

## Русский

Артефакты пока готовятся из исходников в Maven Local и не загружаются из Maven Central. Нужен JDK 17+ и Gradle wrapper из репозитория. Тестам требуется новая отдельная база с суффиксом _test: при существующей таблице очереди они отказываются продолжать.

```bash
./scripts/test-isolated.sh -PtestJavaVersion=17
./gradlew publishToMavenLocal
```

Первая команда запускает свою PostgreSQL 17 в Docker, выполняет сборку и удаляет только этот контейнер. Docker должен работать. Альтернатива: задайте PG_TASK_TEST_URL, PG_TASK_TEST_USER и PG_TASK_TEST_PASSWORD для своей новой временной базы и выполните ./gradlew build. Не используйте рабочие данные. Локальная публикация включает исходники и Javadoc.

База компиляции — Java 17; -PtestJavaVersion=21, 25 или 27 с установленным соответствующим JDK проверяет другую JVM. [Подробности](docs/JAVA-SUPPORT.md). Матрица не запускается автоматически.

[Обычный Java-потребитель](docs/0.3.0/reports.ru.md) использует Maven; [Spring Boot starter](https://github.com/dlorian59/pg-task-lease-spring-boot-starter/blob/main/CONTRIBUTING.md) содержит Maven- и Gradle-сборки потребителя и настоящую HTTP-проверку.
