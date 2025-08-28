package osplus.syncdb2.spring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import osplus.syncdb2.core.domain.SyncEnvelope;
import osplus.syncdb2.core.exception.MqSendException;
import osplus.syncdb2.core.exec.SendToMqRepository;

import java.sql.Connection;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Zweck
 * -----
 * Unit-Tests für {@link SendToMqPortImpl}: Erfolg und Fehlerpfad (status != 0).
 */
public class SendToMqPortImplTest {

    @Test
    @DisplayName("sendWithinTx: status=0 -> OK")
    void send_ok() throws Exception {
        SendToMqRepository repo = Mockito.mock(SendToMqRepository.class);
        Mockito.when(repo.callSendToMq(Mockito.any(), Mockito.anyString()))
                .thenReturn(new SendToMqRepository.Result(0, null));

        SendToMqPortImpl port = new SendToMqPortImpl(repo);
        Connection c = Mockito.mock(Connection.class);
        SyncEnvelope env = new SyncEnvelope("m", "C", "M", Map.of("k", 1), null, null, "v1", "corr", Instant.now());

        port.sendWithinTx(c, env);
        Mockito.verify(repo, Mockito.times(1)).callSendToMq(Mockito.eq(c), Mockito.anyString());
    }

    @Test
    @DisplayName("sendWithinTx: status!=0 -> MqSendException")
    void send_error() throws Exception {
        SendToMqRepository repo = Mockito.mock(SendToMqRepository.class);
        Mockito.when(repo.callSendToMq(Mockito.any(), Mockito.anyString()))
                .thenReturn(new SendToMqRepository.Result(99, "boom"));

        SendToMqPortImpl port = new SendToMqPortImpl(repo);
        Connection c = Mockito.mock(Connection.class);
        SyncEnvelope env = new SyncEnvelope("m", "C", "M", Map.of(), null, null, "v1", "corr", Instant.now());

        assertThatThrownBy(() -> port.sendWithinTx(c, env))
                .isInstanceOf(MqSendException.class)
                .hasMessageContaining("status=99");
    }
}
