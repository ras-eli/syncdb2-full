package osplus.syncdb2.core.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import osplus.syncdb2.core.domain.SyncEnvelope;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Zweck
 * -----
 * Parst ein JSON-Array von Envelopes (wie von READ_FROM_MQ geliefert) in
 * eine Liste von {@link SyncEnvelope}-Objekten.
 * <p>
 * Hinweise
 * --------
 * - Erwartet eine Array-Repräsentation: `[ {...}, {...} ]`.
 * - Fehlende Felder führen zu einer Validierungs-Exception (projektabhängig umsetzbar).
 */
public final class EnvelopeParser {

    private final ObjectMapper mapper;

    public EnvelopeParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<SyncEnvelope> fromJsonArray(String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank()) return Collections.emptyList();
        try {
            return mapper.readValue(jsonArray, new TypeReference<List<SyncEnvelope>>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("Konnte Envelopes-Array nicht parsen", e);
        }
    }
}
