package osplus.syncdb2.core.exec;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Zweck
 * ------
 * Adapter-Schnittstelle als Brücke zu einer **unveränderlichen** SQLJ-Klasse
 * und einer **konkreten Methode** innerhalb dieser Klasse. Diese Abstraktion
 * erlaubt es, SQLJ-Aufrufe generisch, testbar und unabhängig vom übrigen
 * Kontrollfluss (imperativer Service-Flow) zu kapseln.
 * <p>
 * Einsatz / Verhalten
 * -------------------
 * - Der Adapter kennt die Ziel-SQLJ-Klasse (`sqljClassName`) und die Ziel-Methode
 * (`sqljMethodName`).
 * - Er nimmt Eingabeparameter als `Map<String,Object>` entgegen (flexibel, serialisierbar),
 * kann sie in `normalizeParams` validieren/normalisieren und führt anschließend die
 * eigentliche SQLJ-Methode gegen eine bereitgestellte JDBC-`Connection` aus.
 * - Die Transaktionsgrenze liegt **nicht** hier, sondern im umgebenden Orchestrierungs-/Service-Layer.
 * <p>
 * Verwendung (Beispiel)
 * ---------------------
 * <pre>
 * // Im Service-Flow des Entwicklers (imperativ):
 * Connection conn = /* Primary-Connection * / ...;
 * Map<String,Object> p = Map.of("orderId", 123L, "amount", new BigDecimal("42.50"));
 *
 * SqljAdapter<Integer> insertOrder = new InsertOrderAdapter(); // projektspezifische Implementierung
 * Integer rows = insertOrder.execute(conn, p);
 * </pre>
 * <p>
 * Hinweise
 * --------
 * - `sqljClassName`/`sqljMethodName` bilden zusammen die Call-Identität (wichtig für Replay/Registry).
 * - Parameter sollten so gewählt werden, dass sie JSON-serialisierbar sind (für SyncEnvelope/Replay).
 */
public interface SqljAdapter<R> {

    /**
     * Eindeutige Kennung der Ziel-SQLJ-Klasse (z. B. FQN oder logischer Name).
     */
    String sqljClassName();

    /**
     * Eindeutige Kennung der Ziel-SQLJ-Methode innerhalb der Klasse.
     */
    String sqljMethodName();

    /**
     * Optional: Normalisierung/Validierung von Eingabeparametern.
     */
    default Map<String, Object> normalizeParams(Map<String, Object> raw) {
        return raw;
    }

    /**
     * Führt die angegebene SQLJ-Methode gegen die bereitgestellte JDBC-Connection aus.
     *
     * @param connection JDBC-Connection (z. B. aus Primary-DB2-DataSource)
     * @param params     Eingabeparameter (JSON-serialisierbar halten)
     * @return Ergebnistyp R (z. B. RowCount, DTO ...)
     * @throws SQLException bei Datenbankfehlern
     */
    R execute(Connection connection, Map<String, Object> params) throws SQLException;
}
