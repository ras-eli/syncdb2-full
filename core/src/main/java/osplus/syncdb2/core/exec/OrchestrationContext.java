package osplus.syncdb2.core.exec;

import osplus.syncdb2.core.domain.SyncEnvelope;

import java.util.Map;

/**
 * Zweck
 * -----
 * Kontext für die **imperative Ausführung** innerhalb einer Primary-Transaktion.
 * Jeder **erfolgreiche** SQLJ-Aufruf über {@code executeSqlj} erzeugt automatisch
 * ein {@link SyncEnvelope} und löst **sendToMq** innerhalb **derselben Transaktion**
 * aus (Default-Verhalten).
 * <p>
 * Verwendung
 * ----------
 * <pre>
 * ctx.executeSqlj("com.company.sqlj.OrderSqlj", "insertOrder",
 *     Map.of("orderId", 123L, "amount", new java.math.BigDecimal("42.50")));
 *
 * // Optional: Auto-Send deaktivieren und manuell senden
 * ctx.executeSqlj("com.company.sqlj.OrderSqlj", "insertOrder",
 *     Map.of("orderId", 123L), /* disableAutoSend = * / true);
 * SyncEnvelope env = /* Envelope-Fabrik/Helper * / null;
 * ctx.sendToMqManually(env);
 * </pre>
 * <p>
 * Hinweise
 * --------
 * - Fehler in SQLJ **oder** im automatischen sendToMq führen zum **Rollback**
 * der gesamten Transaktion (All-or-Nothing).
 * - Der manuelle Pfad ist **nicht** Default und sollte nur in Sonderfällen genutzt werden.
 */
public interface OrchestrationContext {

    /**
     * Führt eine SQLJ-Operation aus und löst bei Erfolg automatisch sendToMq aus.
     */
    <T> T executeSqlj(String sqljClassName, String sqljMethodName, Map<String, Object> params);

    /**
     * Variante mit optionaler Deaktivierung des Auto-Send-Verhaltens.
     *
     * @param disableAutoSend true = Nur SQLJ ausführen; sendToMq nicht automatisch aufrufen
     */
    <T> T executeSqlj(String sqljClassName, String sqljMethodName, Map<String, Object> params, boolean disableAutoSend);

    /**
     * Manueller Aufruf von sendToMq innerhalb derselben Transaktion (Sonderfälle).
     */
    void sendToMqManually(SyncEnvelope envelope);
}
