package osplus.syncdb2.sampleboot;

import org.springframework.stereotype.Component;
import osplus.syncdb2.core.exec.SqljAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * Beispiel-Adapter, der INSERT in ORDERS ausführt.
 * sqljClassName = "demo.sqlj.OrderSqlj"
 * sqljMethodName = "insertOrder"
 */
@Component
public class InsertOrderAdapter implements SqljAdapter<Integer> {

    @Override
    public String sqljClassName() {
        return "demo.sqlj.OrderSqlj";
    }

    @Override
    public String sqljMethodName() {
        return "insertOrder";
    }

    @Override
    public Integer execute(Connection connection, Map<String, Object> params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO ORDERS(note) VALUES (?)")) {
            ps.setString(1, String.valueOf(params.getOrDefault("note", "n/a")));
            return ps.executeUpdate();
        }
    }
}
