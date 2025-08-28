package osplus.syncdb2.spring;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import osplus.syncdb2.core.exec.SqljAdapter;
import osplus.syncdb2.core.exec.SqljRegistry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

/**
 * Zweck
 * -----
 * Führt einen SQLJ-Adapter **innerhalb der aktuellen Spring-Transaktion**
 * auf derselben, von Spring gebundenen Connection aus.
 * <p>
 * Eigenschaften
 * -------------
 * - Verwendet {@link DataSourceUtils#getConnection(DataSource)}, um die
 * transaktionale Connection zu erhalten (Commit/Rollback obliegt dem TM).
 * - Wirft Exceptions weiter, sodass Spring das Rollback steuert.
 * - Kein Orchestrator nötig; Services rufen direkt diese API auf.
 * <p>
 * Verwendung
 * ----------
 * <pre>{@code
 * R r = SqljExecutorSpring.execute(registry, primaryDataSource,
 *         "com.company.sqlj.OrderSqlj", "insertOrder", Map.of("orderId", 123L));
 * }</pre>
 */
public final class SqljExecutorSpring {

    private SqljExecutorSpring() {
    }

    @SuppressWarnings("unchecked")
    public static <R> R execute(SqljRegistry registry,
                                DataSource dataSource,
                                String sqljClassName,
                                String sqljMethodName,
                                Map<String, Object> params) throws SQLException {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(sqljClassName, "sqljClassName");
        Objects.requireNonNull(sqljMethodName, "sqljMethodName");

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Keine aktive Spring-Transaktion; @Transactional erforderlich.");
        }

        var adapterOpt = registry.findAdapter(sqljClassName, sqljMethodName);
        var adapter = adapterOpt.orElseThrow(() ->
                new IllegalStateException("Kein SqljAdapter für " + sqljClassName + "::" + sqljMethodName));

        Connection c = DataSourceUtils.getConnection(dataSource);
        return ((SqljAdapter<R>) adapter).execute(c, params);
    }
}
