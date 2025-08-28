package osplus.syncdb2.core.exec.impl;

import osplus.syncdb2.core.exec.SqljAdapter;
import osplus.syncdb2.core.exec.SqljRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zweck
 * -----
 * Einfache, thread-sichere In-Memory-Implementierung von {@link SqljRegistry}.
 * Nutzt eine {@link ConcurrentHashMap}, wobei der Schlüssel als
 * "<Klassenname>::<Methodenname>" gebildet wird.
 * <p>
 * Eigenschaften
 * -------------
 * - **Thread-Sicherheit**: Gleichzeitige Lese-/Schreibzugriffe sind erlaubt.
 * - **Fail-Fast**: Bei doppelter Registrierung derselben Kombination wird eine
 * {@link IllegalStateException} geworfen, um Konfigurationsfehler sofort sichtbar zu machen.
 * - **Performanz**: Lookup und Registrierung laufen in O(1).
 * <p>
 * Design-Hinweise
 * ---------------
 * - Versionierung wird absichtlich nicht in den Schlüssel aufgenommen.
 * - Typische Nutzung in Spring: Registry als Singleton-Bean, und alle Adapter
 * werden beim Application-Startup registriert.
 * <p>
 * Beispiel
 * --------
 * <pre>{@code
 * SqljRegistry reg = new InMemorySqljRegistry();
 * reg.register(new InsertOrderAdapter());
 * Optional<SqljAdapter<?>> adapter = reg.findAdapter("com.company.sqlj.OrderSqlj", "insertOrder");
 * }</pre>
 */
public class InMemorySqljRegistry implements SqljRegistry {

    private final Map<String, SqljAdapter<?>> registry = new ConcurrentHashMap<>();

    private static String key(String cls, String mtd) {
        return cls + "::" + mtd;
    }

    @Override
    public <R> void register(SqljAdapter<R> adapter) {
        String k = key(adapter.sqljClassName(), adapter.sqljMethodName());
        SqljAdapter<?> existing = registry.putIfAbsent(k, adapter);
        if (existing != null && existing != adapter) {
            throw new IllegalStateException("Doppelte Registrierung für Schlüssel " + k
                    + " (bestehend=" + existing.getClass().getName()
                    + ", neu=" + adapter.getClass().getName() + ")");
        }
    }

    @Override
    public Optional<SqljAdapter<?>> findAdapter(String className, String methodName) {
        return Optional.ofNullable(registry.get(key(className, methodName)));
    }

    /**
     * Nur für Testzwecke/Diagnose.
     *
     * @return Anzahl der registrierten Adapter
     */
    int size() {
        return registry.size();
    }
}
