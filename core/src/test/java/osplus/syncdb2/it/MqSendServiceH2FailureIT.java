package osplus.syncdb2.it;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import osplus.syncdb2.core.domain.SyncEnvelope;
import osplus.syncdb2.core.exception.MqSendException;
import osplus.syncdb2.core.exec.SendToMqRepository;
import osplus.syncdb2.core.exec.impl.MqSendServiceImpl;

import java.sql.Connection;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Zweck
 * -----
 * Integrations-Test: Fehlerpfad von MqSendServiceImpl mit H2-Repository (simulierter Fehlerstatus).
 * Erwartung: MqSendException wird geworfen.
 */
public class MqSendServiceH2FailureIT {

    static Connection conn;

    @BeforeAll
    static void setupDb() throws Exception {
        conn = ItDatabase.newH2Connection("send_fail");
    }

    @AfterAll
    static void tearDown() throws Exception {
        conn.close();
    }

    @Test
    void send_throwsOnErrorStatus() {
        SendToMqRepository repo = new SendToMqRepositoryH2(true);
        MqSendServiceImpl svc = new MqSendServiceImpl(repo, conn);
        SyncEnvelope env = new SyncEnvelope("m-it-2", "C", "M", Map.of(), "sc", null, "v1", "corr", Instant.now());

        assertThatThrownBy(() -> svc.send(env)).isInstanceOf(MqSendException.class);
    }
}
