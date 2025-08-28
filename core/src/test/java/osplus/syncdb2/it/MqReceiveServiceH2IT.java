package osplus.syncdb2.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import osplus.syncdb2.core.domain.SyncEnvelope;
import osplus.syncdb2.core.exec.*;
import osplus.syncdb2.core.exec.impl.MqReceiveServiceImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zweck
 * -----
 * Integrations-Test: MqReceiveServiceImpl mit H2 für INBOX/processed_message.
 * Erwartung: JSON-Envelope wird aus INBOX gelesen, Adapter ausgeführt und processed markiert.
 */
public class MqReceiveServiceH2IT {
    static Connection conn;
    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .build();

    @BeforeAll
    static void setupDb() throws Exception {
        conn = osplus.syncdb2.it.ItDatabase.newH2Connection("receive_ok");
    }

    @AfterAll
    static void tearDown() throws Exception {
        conn.close();
    }

    @Test
    void pollOnce_processesAndMarks() throws Exception {
        // INBOX vorbereiten: einen Envelope als JSON ablegen
        SyncEnvelope env = new SyncEnvelope("m-it-3", "C", "M", Map.of("k", 1), "sc", 1, "v1", "corr", Instant.parse("2025-08-27T10:00:00Z"));
        String json = "{\"messageId\":\"m-it-3\",\"sqljClassName\":\"C\",\"sqljMethodName\":\"M\",\"params\":{\"k\":1},\"version\":\"v1\",\"correlationId\":\"corr\",\"createdAt\":\"2025-08-27T10:00:00Z\"}";
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO INBOX(envelope_json) VALUES (?)")) {
            ps.setString(1, json);
            ps.executeUpdate();
        }

        // Registry/Adapter vorbereiten
        SqljRegistry registry = Mockito.mock(SqljRegistry.class);
        @SuppressWarnings("unchecked")
        SqljAdapter<Object> adapter = Mockito.mock(SqljAdapter.class);
        Mockito.when(registry.findAdapter("C", "M")).thenReturn(Optional.of(adapter));

        SecondarySqljExecutor executor = new SecondarySqljExecutor(registry, conn);
        ReadFromMqRepository repo = new ReadFromMqRepositoryH2();
        ProcessedMessageRepository processed = new ProcessedMessageRepository();
        MqReceiveServiceImpl svc = new MqReceiveServiceImpl(repo, processed, executor, conn, 10, mapper);

        svc.pollOnce();

        // processed_message sollte den Eintrag haben
        var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM processed_message WHERE message_id='m-it-3'");
        rs.next();
        assertThat(rs.getInt(1)).isEqualTo(1);

        // Adapter sollte ausgeführt worden sein
        Mockito.verify(adapter, Mockito.times(1)).execute(Mockito.eq(conn), Mockito.anyMap());
    }
}
