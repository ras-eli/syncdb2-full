package osplus.syncdb2.core.exec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import osplus.syncdb2.core.domain.SyncEnvelope;

import java.sql.Connection;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Zweck
 * -----
 * Tests für SecondarySqljExecutor: Auflösen des Adapters und Ausführung.
 */
public class SecondarySqljExecutorTest {

    @Test
    @DisplayName("execute: Kein Adapter registriert -> IllegalStateException")
    void noAdapter_registered() {
        SqljRegistry reg = Mockito.mock(SqljRegistry.class);
        Connection conn = Mockito.mock(Connection.class);
        Mockito.when(reg.findAdapter(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.empty());

        SecondarySqljExecutor ex = new SecondarySqljExecutor(reg, conn);
        SyncEnvelope env = new SyncEnvelope("m1", "C", "M", Map.of(), null, null, "v1", "c1", Instant.now());

        assertThatThrownBy(() -> ex.execute(env))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("execute: Adapter gefunden -> execute wird aufgerufen")
    void adapter_found_executes() throws Exception {
        SqljRegistry reg = Mockito.mock(SqljRegistry.class);
        @SuppressWarnings("unchecked")
        SqljAdapter<Object> adapter = Mockito.mock(SqljAdapter.class);
        Mockito.when(reg.findAdapter("C", "M")).thenReturn(Optional.of(adapter));

        Connection conn = Mockito.mock(Connection.class);
        SecondarySqljExecutor ex = new SecondarySqljExecutor(reg, conn);

        SyncEnvelope env = new SyncEnvelope("m1", "C", "M", Map.of("k", 1), null, null, "v1", "c1", Instant.now());
        ex.execute(env);

        Mockito.verify(adapter, Mockito.times(1)).execute(Mockito.eq(conn), Mockito.anyMap());
    }
}
