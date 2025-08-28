package osplus.syncdb2.core.exec;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Zweck
 * -----
 * Kapselt den **JDBC-Aufruf** der Stored Procedure `READ_FROM_MQ`.
 * Erwartete logische Signatur:
 * <p>
 * CALL READ_FROM_MQ(IN P_MAX_BATCH INT, OUT P_ENVELOPES_JSON CLOB, OUT P_STATUS INT, OUT P_ERROR_MSG VARCHAR(1024))
 * <p>
 * Hinweise
 * --------
 * - Diese Methode öffnet **keine** Transaktion; sie läuft außerhalb der Primary-Tx
 * und verwendet eine Secondary-Connection.
 */
public class ReadFromMqRepository {

    public Result callReadFromMq(Connection connection, int maxBatch) throws SQLException {
        try (CallableStatement cs = connection.prepareCall("{ CALL READ_FROM_MQ(?, ?, ?, ?) }")) {
            cs.setInt(1, maxBatch);
            cs.registerOutParameter(2, Types.CLOB);     // je nach Treiber ggf. VARCHAR
            cs.registerOutParameter(3, Types.INTEGER);
            cs.registerOutParameter(4, Types.VARCHAR);
            cs.execute();
            String json = cs.getString(2);
            int status = cs.getInt(3);
            String err = cs.getString(4);
            return new Result(json, status, err);
        }
    }

    public record Result(String envelopesJson, int status, String errorMessage) {
    }
}
