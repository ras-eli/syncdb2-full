package osplus.syncdb2.core.domain;

import java.time.Instant;
import java.util.Map;

/**
 * Zweck
 * ------
 * Repräsentiert das **deterministische Payload** für die exakte Wiederholung (Replay)
 * eines SQLJ-Aufrufs auf der Secondary-DB. Dient zugleich als "Transactional-Outbox"
 * -Nachricht, die nach erfolgreichem SQLJ-Call auf Primary erzeugt und über `sendToMq`
 * persistiert/weitergereicht wird.
 * <p>
 * Einsatz / Verhalten
 * -------------------
 * - Enthält die Identität des Calls: `sqljClassName` + `sqljMethodName` + `params`.
 * - `messageId` ist ein Hash über deterministische Felder (z. B. SHA-256), um Idempotenz
 * auf Secondary sicherzustellen (Mehrfache Zustellung führt nicht zu Mehrfachausführung).
 * - Optionale Tracing-Felder (`scenarioId`, `stepIndex`, `correlationId`) erleichtern Beobachtbarkeit.
 * <p>
 * Verwendung (Beispiel)
 * ---------------------
 * <pre>
 * Map<String,Object> params = Map.of("orderId", 123L, "amount", new BigDecimal("42.50"));
 * String msgId = Hashing.sha256Of("com.company.sqlj.OrderSqlj", "insertOrder", params, "v1");
 * SyncEnvelope env = new SyncEnvelope(
 *     msgId, "com.company.sqlj.OrderSqlj", "insertOrder", params,
 *     "purchase-2025-08-27", 1, "v1", "corr-abc-123", Instant.now()
 * );
 * // env -> JSON -> sendToMq
 * </pre>
 * <p>
 * Hinweise
 * --------
 * - Felder sollten **JSON-serialisierbar** sein (Parametertypen sorgfältig wählen).
 * - `createdAt` möglichst serverseitig setzen (konsistente Zeitquelle).
 */
public record SyncEnvelope(
        String messageId,
        String sqljClassName,
        String sqljMethodName,
        Map<String, Object> params,
        String scenarioId,        // optional
        Integer stepIndex,        // optional
        String version,           // z. B. "v1"
        String correlationId,
        Instant createdAt
) {
}
