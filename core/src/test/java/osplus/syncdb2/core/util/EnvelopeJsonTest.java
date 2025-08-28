package osplus.syncdb2.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import osplus.syncdb2.core.domain.SyncEnvelope;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zweck
 * -----
 * Unit-Tests für EnvelopeJson: kanonische JSON-Serialisierung von SyncEnvelope
 * sowie Hash-relevante Teilserialisierung.
 */
public class EnvelopeJsonTest {

    @Test
    @DisplayName("toCanonicalJson: Enthält alle relevanten Felder in stabiler Reihenfolge")
    void toCanonicalJson_containsFields() {
        SyncEnvelope env = new SyncEnvelope(
                "mid-1", "com.company.sqlj.OrderSqlj", "insertOrder",
                Map.of("orderId", 123L), "scn-1", 1, "v1", "corr-1", Instant.parse("2025-08-27T10:00:00Z")
        );
        String json = EnvelopeJson.toCanonicalJson(env);
        assertThat(json).contains("\"messageId\":\"mid-1\"");
        assertThat(json).contains("\"sqljClassName\":\"com.company.sqlj.OrderSqlj\"");
        assertThat(json).contains("\"sqljMethodName\":\"insertOrder\"");
        assertThat(json).contains("\"version\":\"v1\"");
        assertThat(json).contains("\"createdAt\":\"2025-08-27T10:00:00Z\"");
    }

    @Test
    @DisplayName("toCanonicalJsonForHash: Enthält keine flüchtigen Felder (z. B. createdAt)")
    void toCanonicalJsonForHash_excludesVolatile() {
        String json = EnvelopeJson.toCanonicalJsonForHash(
                "com.company.sqlj.OrderSqlj", "insertOrder", Map.of("orderId", 1L), "v1"
        );
        assertThat(json).contains("\"sqljClassName\"");
        assertThat(json).doesNotContain("createdAt");
    }
}
