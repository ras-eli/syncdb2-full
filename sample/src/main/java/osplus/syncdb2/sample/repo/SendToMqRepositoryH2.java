package osplus.syncdb2.sample.repo;

import osplus.syncdb2.core.exec.SendToMqRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Zweck
 * -----
 * H2-Implementierung des Send-Repository: Schreibt das Envelope-JSON direkt in OUTBOX.
 * Dient als Ersatz für einen echten Stored-Procedure-Aufruf in diesem Sample.
 */
public class SendToMqRepositoryH2 extends SendToMqRepository {

    @Override
    public Result callSendToMq(Connection connection, String envelopeJson) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO OUTBOX(envelope_json) VALUES (?)")) {
            ps.setString(1, envelopeJson);
            ps.executeUpdate();
            return new Result(0, null);
        }
    }
}
