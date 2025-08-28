package osplus.syncdb2.core.util;

import osplus.syncdb2.core.domain.SyncEnvelope;

import java.time.Instant;
import java.util.Map;

/**
 * Zweck
 * -----
 * Fabrik zum Erzeugen von {@link SyncEnvelope}-Instanzen mit deterministischer
 * `messageId`-Bildung. Die Hash-Bildung erfolgt über **kanonisches JSON** der
 * stabilen Felder (sqljClassName, sqljMethodName, params, version).
 * <p>
 * Verwendung
 * ----------
 * <pre>
 * Map<String,Object> params = Map.of("orderId", 123L);
 * SyncEnvelope env = EnvelopeFactory.create(
 *     "com.company.sqlj.OrderSqlj", "insertOrder", params,
 *     "v1", "corr-123", null, null
 * );
 * </pre>
 * <p>
 * Hinweise
 * --------
 * - Die Kanonisierung der JSON-Repräsentation muss **stabile Schlüsselreihenfolge**
 * sicherstellen. Dies kann z. B. durch eine Jackson-Konfiguration mit sortierten
 * Schlüsseln erreicht werden (Implementierung projektspezifisch).
 */
public final class EnvelopeFactory {

    private EnvelopeFactory() {
    }

    public static SyncEnvelope create(
            String sqljClassName,
            String sqljMethodName,
            Map<String, Object> params,
            String version,
            String correlationId,
            String scenarioId,
            Integer stepIndex
    ) {
        String canonicalJson = CanonicalJson.of(sqljClassName, sqljMethodName, params, version);
        String messageId = HashingUtil.sha256Hex(canonicalJson);
        return new SyncEnvelope(
                messageId,
                sqljClassName,
                sqljMethodName,
                params,
                scenarioId,
                stepIndex,
                version,
                correlationId,
                Instant.now()
        );
    }

    /**
     * Platzhalter für eine kanonische JSON-Serialisierung (stabile Reihenfolge).
     * Die konkrete Implementierung (z. B. Jackson mit sortierten Keys) wird im
     * Projekt geliefert; hier nur die Signatur als Hinweis.
     */
    static final class CanonicalJson {
        static String of(String cls, String mth, Map<String, Object> params, String version) {
            // Pseudocode/Platzhalter: in der echten Implementierung JSON mit sortierten Keys erzeugen
            String sb = "{" +
                    "'sqljClassName':'" + cls + "'," +
                    "'sqljMethodName':'" + mth + "'," +
                    "'version':'" + version + "'," +
                    "'params':" + params + // TODO: echte JSON-Kanonisierung
                    "}";
            return sb;
        }
    }
}
