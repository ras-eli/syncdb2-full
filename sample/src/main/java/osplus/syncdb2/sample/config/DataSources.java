package osplus.syncdb2.sample.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Zweck
 * -----
 * Stellt H2-DataSources für Primary und Secondary bereit und initialisiert
 * die Tabellen OUTBOX, INBOX und processed_message. Dieses Setup dient ausschließlich
 * dem **Sample/Beispielbetrieb** (nicht für Produktion).
 */
public final class DataSources {

    private DataSources() {
    }

    public static DataSource primary() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:h2:mem:primary;DB_CLOSE_DELAY=-1");
        cfg.setMaximumPoolSize(4);
        return new HikariDataSource(cfg);
    }

    public static DataSource secondary() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:h2:mem:secondary;DB_CLOSE_DELAY=-1");
        cfg.setMaximumPoolSize(4);
        return new HikariDataSource(cfg);
    }

    public static void initSchema(DataSource ds) throws SQLException {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS OUTBOX (id IDENTITY PRIMARY KEY, envelope_json CLOB, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS INBOX  (id IDENTITY PRIMARY KEY, envelope_json CLOB, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS processed_message (message_id VARCHAR(128) PRIMARY KEY, processed_at TIMESTAMP)");
        }
    }
}
