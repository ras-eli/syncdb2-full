package osplus.syncdb2.core.exec.impl;

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
 * Unit-Tests für MqSendServiceImpl: Erfolg und Fehlerpfad (Status != 0).
 */
public class MqSendServiceImplTest {

    @Test
    @DisplayName("send: Erfolgsfall -> kein Fehler, Repository wird aufgerufen")
    void send_success() throws Exception {
        SendToMqRepository repo = Mockito.mock(SendToMqRepository.class);
        Connection conn = Mockito.mock(Connection.class);
        Mockito.when(repo.callSendToMq(Mockito.eq(conn), Mockito.anyString()))
                .thenReturn(new SendToMqRepository.Result(0, null));

        MqSendServiceImpl svc = new MqSendServiceImpl(repo, conn);
        SyncEnvelope env = new SyncEnvelope("m1", "C", "M", Map.of("k", 1), null, null, "v1", "c1", Instant.now());

        svc.send(env); // sollte kein Exception werfen
        Mockito.verify(repo, Mockito.times(1)).callSendToMq(Mockito.eq(conn), Mockito.anyString());
    }

    @Test
    @DisplayName("send: Fehlerstatus -> MqSendException und Rollback durch aufrufenden Tx")
    void send_errorStatus() throws Exception {
        SendToMqRepository repo = Mockito.mock(SendToMqRepository.class);
        Connection conn = Mockito.mock(Connection.class);
        Mockito.when(repo.callSendToMq(Mockito.eq(conn), Mockito.anyString()))
                .thenReturn(new SendToMqRepository.Result(123, "boom"));

        MqSendServiceImpl svc = new MqSendServiceImpl(repo, conn);
        SyncEnvelope env = new SyncEnvelope("m1", "C", "M", Map.of(), null, null, "v1", "c1", Instant.now());

        assertThatThrownBy(() -> svc.send(env))
                .isInstanceOf(MqSendException.class);
    }
}
