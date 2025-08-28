package osplus.syncdb2.spring;

import osplus.syncdb2.core.domain.SyncEnvelope;
import osplus.syncdb2.core.exec.SendToMqRepository;
import osplus.syncdb2.core.util.EnvelopeJson;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Zweck
 * -----
 * Standard-Implementierung von {@link MqSendPort}, die die Outbox-Logik
 * an das Repository {@link SendToMqRepository} delegiert.
 * <p>
 * Details
 * -------
 * - Serialisiert das Envelope mit {@code EnvelopeJson} (kanonische, stabile
 * Darstellung) und ruft das Repository mit der **transaktionalen Connection**
 * auf.
 * - Statusprüfung und Fehlerbehandlung sind Aufgabe des Repositories/Service.
 */
public class SendToMqPortImpl implements MqSendPort {

    private final SendToMqRepository repository;

    public SendToMqPortImpl(SendToMqRepository repository) {
        this.repository = repository;
    }

    @Override
    public void sendWithinTx(Connection connection, SyncEnvelope envelope) throws SQLException {
        String json = EnvelopeJson.toCanonicalJson(envelope);
        repository.callSendToMq(connection, json);
    }
}
