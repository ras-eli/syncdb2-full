package osplus.syncdb2.sample.sqlj;

import osplus.syncdb2.core.exec.SqljAdapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Zweck
 * -----
 * Beispiel-Adapter für com.company.sqlj.LimitSqlj#assignLimits.
 */
public class AssignLimitsAdapter implements SqljAdapter<Integer> {

    @Override
    public String sqljClassName() {
        return "com.company.sqlj.LimitSqlj";
    }

    @Override
    public String sqljMethodName() {
        return "assignLimits";
    }

    @Override
    public Integer execute(Connection connection, Map<String, Object> params) throws SQLException {
        return 1;
    }
}
