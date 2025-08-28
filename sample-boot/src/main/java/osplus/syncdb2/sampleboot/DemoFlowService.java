package osplus.syncdb2.sampleboot;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import osplus.syncdb2.core.domain.SyncEnvelope;
import osplus.syncdb2.core.exec.SqljRegistry;
import osplus.syncdb2.core.util.EnvelopeFactory;
import osplus.syncdb2.spring.SpringTxOutbox;
import osplus.syncdb2.spring.SqljExecutorSpring;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

/**
 * Service mit **vollständig imperativem** Flow:
 * - mehrere SQLJ-Aufrufe innerhalb EINER @Transactional-Methode
 * - optionale Outbox-Events, die vor Commit gesendet werden
 */
@Service
public class DemoFlowService {

    private final SqljRegistry registry;
    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public DemoFlowService(SqljRegistry registry,
                           @Qualifier("dataSource") DataSource dataSource,
                           JdbcTemplate jdbcTemplate) {
        this.registry = registry;
        this.dataSource = dataSource;
        this.jdbc = jdbcTemplate;
    }

    @Transactional
    public void runHappyPath() throws SQLException {
        // SQLJ #1
        SqljExecutorSpring.execute(registry, dataSource,
                "demo.sqlj.OrderSqlj", "insertOrder", Map.of("note", "A"));

        // ... beliebige Logik dazwischen (z. B. Logging, Berechnungen) ...
        jdbc.update("INSERT INTO AUDIT(msg) VALUES (?)", "after A");

        // SQLJ #2
        SqljExecutorSpring.execute(registry, dataSource,
                "demo.sqlj.OrderSqlj", "insertOrder", Map.of("note", "B"));

        // Outbox (optional): zwei Events
        SyncEnvelope e1 = EnvelopeFactory.create("demo.sqlj.OrderSqlj", "insertOrder", Map.of("note", "A"), "v1", "corr-1", null, null);
        SyncEnvelope e2 = EnvelopeFactory.create("demo.sqlj.OrderSqlj", "insertOrder", Map.of("note", "B"), "v1", "corr-1", null, null);
        SpringTxOutbox.record(e1);
        SpringTxOutbox.record(e2);
    }

    @Transactional
    public void runWithFailureOnSecond() throws SQLException {
        // SQLJ #1 OK
        SqljExecutorSpring.execute(registry, dataSource,
                "demo.sqlj.OrderSqlj", "insertOrder", Map.of("note", "OK"));

        // شکست عمدی: مقدار NOTE = "FAIL" را می‌فرستیم و در Outbox قبل از Commit خطا می‌سازیم
        SyncEnvelope bad = EnvelopeFactory.create("demo.sqlj.OrderSqlj", "insertOrder", Map.of("note", "FAIL"), "v1", "corr-2", null, null);
        SpringTxOutbox.record(bad);

        // برای نمایش Rollback: یک درج دیگر که نباید باقی بماند
        SqljExecutorSpring.execute(registry, dataSource,
                "demo.sqlj.OrderSqlj", "insertOrder", Map.of("note", "AFTER_FAIL"));
    }
}
