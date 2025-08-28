package osplus.syncdb2.core.exec.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import osplus.syncdb2.core.domain.SyncEnvelope;
import osplus.syncdb2.core.exec.ProcessedMessageRepository;
import osplus.syncdb2.core.exec.ReadFromMqRepository;
import osplus.syncdb2.core.exec.SecondarySqljExecutor;

import java.sql.Connection;

/**
 * Zweck
 * -----
 * Unit-Tests für MqReceiveServiceImpl: Batch lesen, parsen, Idempotenz prüfen, ausführen.
 */
public class MqReceiveServiceImplTest {

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .build();

    @Test
    @DisplayName("pollOnce: Status!=0 -> keine Verarbeitung")
    void pollOnce_statusNotOk() throws Exception {
        ReadFromMqRepository repo = Mockito.mock(ReadFromMqRepository.class);
        Mockito.when(repo.callReadFromMq(Mockito.any(), Mockito.anyInt()))
                .thenReturn(new ReadFromMqRepository.Result("[]", 999, "boom"));

        ProcessedMessageRepository processed = Mockito.mock(ProcessedMessageRepository.class);
        SecondarySqljExecutor executor = Mockito.mock(SecondarySqljExecutor.class);
        Connection conn = Mockito.mock(Connection.class);

        MqReceiveServiceImpl svc = new MqReceiveServiceImpl(repo, processed, executor, conn, 10, mapper);
        svc.pollOnce();

        Mockito.verify(executor, Mockito.never()).execute(Mockito.any());
    }

    @Test
    @DisplayName("pollOnce: Verarbeitet neue Envelopes und markiert sie als processed")
    void pollOnce_processNew() throws Exception {
        String json = "[{\"messageId\":\"m1\",\"sqljClassName\":\"C\",\"sqljMethodName\":\"M\",\"params\":{},\"version\":\"v1\",\"correlationId\":\"c1\",\"createdAt\":\"2025-08-27T10:00:00Z\"}]";
        ReadFromMqRepository repo = Mockito.mock(ReadFromMqRepository.class);
        Mockito.when(repo.callReadFromMq(Mockito.any(), Mockito.anyInt()))
                .thenReturn(new ReadFromMqRepository.Result(json, 0, null));

        ProcessedMessageRepository processed = Mockito.mock(ProcessedMessageRepository.class);
        Mockito.when(processed.exists(Mockito.any(), Mockito.eq("m1"))).thenReturn(false);

        SecondarySqljExecutor executor = Mockito.mock(SecondarySqljExecutor.class);
        Connection conn = Mockito.mock(Connection.class);

        MqReceiveServiceImpl svc = new MqReceiveServiceImpl(repo, processed, executor, conn, 10, mapper);
        svc.pollOnce();

        Mockito.verify(executor, Mockito.times(1)).execute(Mockito.any(SyncEnvelope.class));
        Mockito.verify(processed, Mockito.times(1)).markProcessed(Mockito.any(), Mockito.eq("m1"));
    }
}
