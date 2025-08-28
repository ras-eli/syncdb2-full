package osplus.syncdb2.it;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import osplus.syncdb2.core.domain.SyncEnvelope;
import osplus.syncdb2.core.exec.SendToMqRepository;
import osplus.syncdb2.core.exec.impl.MqSendServiceImpl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zweck
 * -----
 * Integrations-Test: MqSendServiceImpl mit H2-Repository (OUTBOX-Schreibpfad).
 * Erwartung: JSON wird in OUTBOX persistiert; Status = 0 (Erfolg).
 */
public class MqSendServiceH2SuccessIT {

    static Connection conn;

    @BeforeAll
    static void setupDb() throws Exception {
        conn = ItDatabase.newH2Connection("send_success");
    }

    @AfterAll
    static void tearDown() throws Exception {
        conn.close();
    }

    @Test
    void send_persistsToOutbox() throws Exception {
        SendToMqRepository repo = new SendToMqRepositoryH2(false);
        MqSendServiceImpl svc = new MqSendServiceImpl(repo, conn);
        SyncEnvelope env = new SyncEnvelope("m-it-1", "C", "M", Map.of("k", 1), "sc", 1, "v1", "corr", Instant.parse("2025-08-27T10:00:00Z"));

        svc.send(env);

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM OUTBOX")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }
}
