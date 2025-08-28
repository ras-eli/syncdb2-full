package osplus.syncdb2.core.exec;

import java.util.Optional;

/**
 * Zweck
 * -----
 * `SqljRegistry` dient als **zentrales Nachschlagewerk** für die Abbildung
 * von (SQLJ-Klassenname, SQLJ-Methodenname) -> konkreter {@link SqljAdapter}.
 * <p>
 * Anwendungskontext
 * -----------------
 * - Insbesondere im **Replay**-Pfad auf der Secondary-DB wird dieses Registry
 * benötigt: dort liegen nur die Metadaten (sqljClassName + sqljMethodName)
 * aus dem {@code SyncEnvelope} vor, und die passende Adapter-Instanz muss
 * ermittelt werden.
 * <p>
 * Motivation
 * ----------
 * - SQLJ-Klassen sind unveränderlich; der Adapter kapselt die konkrete
 * Aufruflogik (Parameterzuordnung, Fehlerbehandlung).
 * - Registry erlaubt eine klare Trennung von Metadaten (im Envelope) und
 * Implementierungsdetails (im Adapter).
 * <p>
 * Designentscheidungen
 * --------------------
 * - **Schlüsselbildung**: Kombination aus `sqljClassName` und `sqljMethodName`.
 * Versionen werden nicht in den Schlüssel aufgenommen, da diese
 * (a) über eigene Methodennamen (`insertOrder_v2`) oder
 * (b) interne Kompatibilität im Adapter
 * behandelt werden sollen.
 * - **Eindeutigkeit**: Jede Kombination darf nur einmal vorkommen.
 * - **Thread-Sicherheit**: Implementierungen müssen gleichzeitigen Zugriff
 * sicher handhaben.
 * <p>
 * Beispiel
 * --------
 * <pre>{@code
 * SqljRegistry reg = new InMemorySqljRegistry();
 * reg.register(new InsertOrderAdapter());
 * Optional<SqljAdapter<?>> opt = reg.findAdapter("com.company.sqlj.OrderSqlj", "insertOrder");
 * opt.orElseThrow().execute(conn, Map.of("orderId", 123L));
 * }</pre>
 */
public interface SqljRegistry {

    /**
     * Registriert einen Adapter für seine (Klasse, Methode)-Kombination.
     *
     * @param adapter Adapter-Instanz
     * @param <R>     Rückgabetyp des Adapters
     * @throws IllegalStateException wenn die Kombination bereits existiert
     */
    <R> void register(SqljAdapter<R> adapter);

    /**
     * Findet den Adapter anhand von Klassen- und Methodennamen.
     *
     * @param className  Vollqualifizierter SQLJ-Klassenname
     * @param methodName SQLJ-Methodenname
     * @return Optional mit Adapter oder leer, falls nicht registriert
     */
    Optional<SqljAdapter<?>> findAdapter(String className, String methodName);
}
