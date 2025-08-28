package osplus.syncdb2.it;

import osplus.syncdb2.core.exec.ReadFromMqRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Zweck
 * -----
 * H2-spezifische Test-Implementierung von {@link ReadFromMqRepository}, die
 * Envelopes als JSON aus der INBOX-Tabelle holt und zu einem JSON-Array zusammenfügt.
 */
public class ReadFromMqRepositoryH2 extends ReadFromMqRepository {

    @Override
    public Result callReadFromMq(Connection connection, int maxBatch) throws SQLException {
        List<String> entries = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT envelope_json FROM INBOX ORDER BY id ASC LIMIT ?")) {
            ps.setInt(1, maxBatch);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(rs.getString(1));
                }
            }
        }
        String jsonArray = "[" + String.join(",", entries) + "]";
        return new Result(jsonArray, 0, null);
    }
}
