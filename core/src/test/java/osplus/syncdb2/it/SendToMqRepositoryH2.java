package osplus.syncdb2.it;

import osplus.syncdb2.core.exec.SendToMqRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Zweck
 * -----
 * H2-spezifische Test-Implementierung von {@link SendToMqRepository}, die **keinen**
 * Stored-Procedure-Call ausführt, sondern direkt in die OUTBOX-Tabelle schreibt.
 * So lassen sich Integrationspfade mit echter JDBC-Verbindung testen.
 */
public class SendToMqRepositoryH2 extends SendToMqRepository {

    private final boolean forceError;

    public SendToMqRepositoryH2() {
        this(false);
    }

    public SendToMqRepositoryH2(boolean forceError) {
        this.forceError = forceError;
    }

    @Override
    public Result callSendToMq(Connection connection, String envelopeJson) throws SQLException {
        if (forceError) {
            return new Result(777, "H2-Fehler (simuliert)");
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO OUTBOX(envelope_json) VALUES (?)")) {
            ps.setString(1, envelopeJson);
            ps.executeUpdate();
            return new Result(0, null);
        }
    }
}
