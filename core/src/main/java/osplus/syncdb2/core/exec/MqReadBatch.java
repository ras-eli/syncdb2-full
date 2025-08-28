package osplus.syncdb2.core.exec;

import osplus.syncdb2.core.domain.SyncEnvelope;

import java.util.List;

/**
 * Zweck
 * -----
 * Repräsentiert das Ergebnis eines einzelnen READ_FROM_MQ-Aufrufs:
 * eine Liste von Envelopes (Batch) plus Status/Fehlertext.
 */
public record MqReadBatch(
        List<SyncEnvelope> envelopes,
        int status,
        String errorMessage
) {
}
