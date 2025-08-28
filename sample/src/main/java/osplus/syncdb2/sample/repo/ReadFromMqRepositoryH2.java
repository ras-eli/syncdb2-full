package osplus.syncdb2.sample.repo;

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
 * H2-Implementierung des Read-Repository: Liest Einträge aus INBOX und liefert
 * ein JSON-Array (Strings werden unverändert zusammengefügt).
 */
public class ReadFromMqRepositoryH2 extends ReadFromMqRepository {

    @Override
    public Result callReadFromMq(Connection connection, int maxBatch) throws SQLException {
        List<String> arr = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT envelope_json FROM INBOX ORDER BY id ASC LIMIT ?")) {
            ps.setInt(1, maxBatch);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    arr.add(rs.getString(1));
                }
            }
        }
        String json = "[" + String.join(",", arr) + "]";
        return new Result(json, 0, null);
    }
}
