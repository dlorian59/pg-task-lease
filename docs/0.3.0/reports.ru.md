# Фоновый отчёт: core API и интеграция Spring Boot

[Версии](../index.md) · [Руководство core](ru.md) · [English](reports.en.md) · [Русский](reports.ru.md) · [README](../../README.md)

Запрос отчёта должен переживать перезапуск процесса и не создавать вторую задачу при повторе отправителя. pg-task-lease сохраняет постоянный UUID и передаёт работнику временное владение строкой. Детерминированный результат по этому UUID делает повтор записи идемпотентным; сама очередь не гарантирует exactly-once внешних действий.

## Запустить пример без Spring

Используйте [ReportExample.java](examples/ReportExample.java) с существующими [pom.xml](examples/pom.xml) и [schema.sql](examples/schema.sql). Подготовьте новую базу по [инструкции](ru.md#8-запустить-пример-и-разобрать-ошибки) и предварительно установите core 0.3.0 через publishToMavenLocal. В каталоге примера с заданным TASKS_R2DBC_URL:

```bash
mvn -q compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
TASK_ID=7a62d0c1-22a8-4936-a00e-99918e3d2d32
java -cp "target/classes:$(cat target/classpath.txt)" ReportExample "$TASK_ID" 2026-Q4
java -cp "target/classes:$(cat target/classpath.txt)" ReportExample "$TASK_ID" 2026-Q4
```

Первый запуск печатает REPORT_DONE и создаёт `reports/<task-id>.csv`; второй — `ALREADY_SUBMITTED state=DONE`. Для действительно нового запроса выберите новый UUID. Содержимое файла:

```csv
quarter,invoices,total
2026-Q4,3,450.00
```

Пример явно отправляет и захватывает задачу, пишет через временный файл и атомарное переименование, подтверждает и проверяет DONE. Ошибки файлового ввода-вывода вызывают fail; неоднозначная ошибка подтверждения не превращается в ошибку формирования файла. Демонстрационные итоги обозначены в исходниках: замените их запросом к данным приложения.

Это одна конечная попытка. Повторная отправка в состоянии READY/LEASED/DEAD только сообщает состояние, а не автоматически выполняет задачу. В рабочем приложении на core хост организует опрос, heartbeat, восстановление, отмену и ограничение параллелизма. Долгому отчёту нужно продление до истечения аренды. Подтверждение может не пройти уже после записи файла, поэтому ключ и содержимое результата должны допускать безопасный повтор.

## Сократить служебный код через Spring Boot

Отдельный [pg-task-lease-spring-boot-starter](https://github.com/dlorian59/pg-task-lease-spring-boot-starter) создаёт PgTaskQueue из ConnectionFactory Boot и добавляет явно включаемого работника. Приложение отчёта версии 0.1.0 использует тот же CSV по UUID, но хосту достаточно поставить @PgTaskHandler("reports") на метод singleton bean (либо зарегистрировать TaskHandlerRegistration) и задать `pg-task-lease.worker.enabled=true`.

Стартер берёт на себя ограниченный опрос, ожидание обработчика, heartbeat, восстановление просроченных аренд, тайм-ауты, повторы и отмену при остановке. Собственные beans имеют приоритет. Миграция базы остаётся явной. Аннотированные методы обнаруживаются у инициализированных singleton beans и вызываются через существующие Spring-прокси. Это регистрация, а не новый аспект. Доменные записи и подтверждение очереди не объединяются в одну транзакцию.

Клонируйте репозитории ядра и стартера в соседние каталоги. Если репозитории находятся рядом, откройте `../pg-task-lease-spring-boot-starter/README.md`, затем `docs/0.1.0/ru.md` и `examples/report-worker/` в этом репозитории. После установки core в Maven Local установите стартер через `./gradlew build publishToMavenLocal` и запустите его `examples/report-worker/run-demo.sh`.

[README](../../README.md) · [Core API](ru.md) · [Версии](../index.md)
