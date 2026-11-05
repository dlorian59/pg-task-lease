import io.github.alex59.pgtasklease.*;
import io.r2dbc.spi.ConnectionFactories;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.UUID;

// A finite core-only report job. Prepare schema.sql in an isolated database first.
// Конечная задача отчёта только на core. Сначала примените schema.sql в отдельной базе.
public final class ReportExample {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: ReportExample <stable-task-uuid> <2026-Q4>");
        UUID id = UUID.fromString(args[0]);
        String quarter = args[1];
        if (!quarter.matches("[0-9]{4}-Q[1-4]")) throw new IllegalArgumentException("Invalid quarter");
        var queue = new PgTaskQueue(ConnectionFactories.get(System.getenv("TASKS_R2DBC_URL")));
        var wait = Duration.ofSeconds(5);
        String name = "report-example-" + id;
        // A repeated producer request retains the same ID and cannot overwrite the existing task.
        // Повторный запрос производителя сохраняет ID и не перезаписывает существующую задачу.
        if (!queue.submit(TaskRequest.ready(id, name, quarter).withMaxAttempts(3)).block(wait)) {
            System.out.println("ALREADY_SUBMITTED state=" + queue.find(id).block(wait).state());
            return;
        }
        TaskLease lease = queue.claim(name, "example-worker", 1, Duration.ofSeconds(30)).block(wait).get(0);
        Path folder = Path.of("reports");
        Path output = folder.resolve(id + ".csv");
        Path temporary = null;
        try {
            Files.createDirectories(folder);
            temporary = Files.createTempFile(folder, id + "-", ".tmp");
            // Demo totals stand in for an application query; the same task produces the same file.
            // Демонстрационные итоги заменяют запрос приложения; одна задача формирует один и тот же файл.
            Files.writeString(temporary, "quarter,invoices,total\n" + lease.payload() + ",3,450.00\n");
            Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException error) {
            queue.fail(lease, Duration.ofSeconds(5), "Report file I/O failed").block(wait);
            throw error;
        } finally { if (temporary != null) Files.deleteIfExists(temporary); }
        // An acknowledgment error is not evidence that file generation failed or SQL rolled back.
        // Ошибка подтверждения не доказывает сбой записи файла или откат SQL.
        if (!queue.complete(lease).block(wait)) throw new IllegalStateException("Lease lost; the report may already exist");
        if (queue.find(id).block(wait).state() != TaskState.DONE) throw new IllegalStateException("Expected DONE");
        System.out.println("REPORT_DONE task=" + id + " path=" + output);
    }
}
