package osplus.syncdb2.sample.sqlj;

import osplus.syncdb2.core.exec.SqljAdapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Zweck
 * -----
 * Beispiel-Adapter für com.company.sqlj.InstallmentSqlj#createInstallment.
 */
public class CreateInstallmentAdapter implements SqljAdapter<Integer> {

    @Override
    public String sqljClassName() {
        return "com.company.sqlj.InstallmentSqlj";
    }

    @Override
    public String sqljMethodName() {
        return "createInstallment";
    }

    @Override
    public Integer execute(Connection connection, Map<String, Object> params) throws SQLException {
        return 1;
    }
}
