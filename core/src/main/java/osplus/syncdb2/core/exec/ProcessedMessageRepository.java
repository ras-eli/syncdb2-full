package osplus.syncdb2.core.exec;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

/**
 * Zweck
 * -----
 * Idempotenzkontrolle für empfangene Envelopes auf Secondary: Prüft, ob eine
 * `messageId` bereits verarbeitet wurde und markiert erfolgreiche Verarbeitungen.
 */
public class ProcessedMessageRepository {

    public boolean exists(Connection connection, String messageId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM processed_message WHERE message_id = ?")) {
            ps.setString(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void markProcessed(Connection connection, String messageId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO processed_message(message_id, processed_at) VALUES (?, ?)")) {
            ps.setString(1, messageId);
            ps.setObject(2, Instant.now());
            ps.executeUpdate();
        }
    }
}
