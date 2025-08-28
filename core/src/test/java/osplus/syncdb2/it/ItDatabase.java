package osplus.syncdb2.it;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Zweck
 * -----
 * Hilfsklasse zum Aufsetzen einer **echten** H2-In-Memory-Datenbank für Integrations-Tests.
 * Erstellt Tabellen OUTBOX, INBOX und processed_message.
 */
public final class ItDatabase {

    private ItDatabase() {
    }

    public static Connection newH2Connection(String dbName) throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS OUTBOX (id IDENTITY PRIMARY KEY, envelope_json CLOB, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS INBOX  (id IDENTITY PRIMARY KEY, envelope_json CLOB, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS processed_message (message_id VARCHAR(128) PRIMARY KEY, processed_at TIMESTAMP)");
        }
        return c;
    }
}
