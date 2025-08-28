package osplus.syncdb2.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import osplus.syncdb2.core.domain.SyncEnvelope;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zweck
 * -----
 * Unit-Tests für EnvelopeParser: JSON-Array -> Liste von SyncEnvelope.
 */
public class EnvelopeParserTest {

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .build();

    @Test
    @DisplayName("fromJsonArray: Leerer/Null-Input -> leere Liste")
    void emptyInput_returnsEmptyList() {
        EnvelopeParser envelopeParser = new EnvelopeParser(mapper);
        assertThat(envelopeParser.fromJsonArray(null)).isEmpty();
        assertThat(envelopeParser.fromJsonArray("")).isEmpty();
    }

    @Test
    @DisplayName("fromJsonArray: Valides Array -> Liste mit Envelopes")
    void parse_validArray() {
        EnvelopeParser envelopeParser = new EnvelopeParser(mapper);
        String json = "[{\"messageId\":\"m1\",\"sqljClassName\":\"C\",\"sqljMethodName\":\"M\",\"params\":{},\"version\":\"v1\",\"correlationId\":\"c1\",\"createdAt\":\"2025-08-27T10:00:00Z\"}]";
        List<SyncEnvelope> list = envelopeParser.fromJsonArray(json);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).messageId()).isEqualTo("m1");
    }
}
