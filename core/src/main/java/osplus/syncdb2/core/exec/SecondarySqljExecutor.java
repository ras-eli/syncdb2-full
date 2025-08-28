package osplus.syncdb2.core.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import osplus.syncdb2.core.domain.SyncEnvelope;

import java.sql.Connection;
import java.util.Map;
import java.util.Optional;

/**
 * Zweck
 * -----
 * Führt ein in {@link SyncEnvelope} beschriebenes SQLJ über die Secondary-Connection aus.
 * Dazu wird der passende {@link SqljAdapter} über die Registry aufgelöst.
 */
public class SecondarySqljExecutor {

    private static final Logger log = LoggerFactory.getLogger(SecondarySqljExecutor.class);

    private final SqljRegistry registry;
    private final Connection secondaryConnection;

    public SecondarySqljExecutor(SqljRegistry registry, Connection secondaryConnection) {
        this.registry = registry;
        this.secondaryConnection = secondaryConnection;
    }

    public void execute(SyncEnvelope env) {
        Optional<SqljAdapter<?>> opt = registry.findAdapter(env.sqljClassName(), env.sqljMethodName());
        if (opt.isEmpty()) {
            throw new IllegalStateException("Kein SqljAdapter registriert für "
                    + env.sqljClassName() + "#" + env.sqljMethodName());
        }
        SqljAdapter<?> adapter = opt.get();
        @SuppressWarnings("unchecked")
        Map<String, Object> params = env.params();
        try {
            adapter.execute(secondaryConnection, params);
        } catch (Exception e) {
            log.error("Fehler bei Secondary-SQLJ-Ausführung für messageId={}", env.messageId(), e);
            throw new RuntimeException("Secondary-Ausführung fehlgeschlagen: " + e.getMessage(), e);
        }
    }
}
