package osplus.syncdb2.sample.sqlj;

import osplus.syncdb2.core.exec.SqljAdapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Zweck
 * -----
 * Beispiel-Adapter für com.company.sqlj.OrderSqlj#insertOrder.
 * Simuliert die Ausführung (hier ohne echte SQLJ-Abhängigkeit).
 */
public class InsertOrderAdapter implements SqljAdapter<Integer> {

    @Override
    public String sqljClassName() {
        return "com.company.sqlj.OrderSqlj";
    }

    @Override
    public String sqljMethodName() {
        return "insertOrder";
    }

    @Override
    public Integer execute(Connection connection, Map<String, Object> params) throws SQLException {
        // Simulation: würde hier OrderSqlj(connection).insertOrder(...) aufrufen
        return 1;
    }
}
