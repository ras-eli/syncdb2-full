package osplus.syncdb2.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import osplus.syncdb2.core.util.EnvelopeFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zweck
 * -----
 * Unit-Tests für EnvelopeFactory: Erzeugung eines SyncEnvelope
 * inklusive deterministischer messageId.
 */
public class EnvelopeFactoryTest {

    @Test
    @DisplayName("create: Erzeugt Envelope mit deterministischer messageId")
    void create_buildsDeterministicMessageId() {
        SyncEnvelope e1 = EnvelopeFactory.create(
                "cls", "mtd", Map.of("k", 1), "v1", "corr", null, null
        );
        SyncEnvelope e2 = EnvelopeFactory.create(
                "cls", "mtd", Map.of("k", 1), "v1", "corr", null, null
        );
        assertThat(e1.messageId()).isEqualTo(e2.messageId());
    }
}
