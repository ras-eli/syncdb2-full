package osplus.syncdb2.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zweck
 * -----
 * Unit-Tests für HashingUtil (SHA-256 als Hex-String).
 * Prüft Stabilität und deterministische Ergebnisse.
 */
public class HashingUtilTest {

    @Test
    @DisplayName("sha256Hex: Liefert deterministischen Hex-String")
    void sha256Hex_deterministic() {
        String a = HashingUtil.sha256Hex("hello");
        String b = HashingUtil.sha256Hex("hello");
        assertThat(a).isEqualTo(b);
        assertThat(a).hasSize(64); // 32 bytes -> 64 hex chars
    }

    @Test
    @DisplayName("sha256Hex: Unterschiedliche Eingaben -> unterschiedliche Hashes")
    void sha256Hex_differs() {
        String a = HashingUtil.sha256Hex("hello");
        String b = HashingUtil.sha256Hex("world");
        assertThat(a).isNotEqualTo(b);
    }
}
