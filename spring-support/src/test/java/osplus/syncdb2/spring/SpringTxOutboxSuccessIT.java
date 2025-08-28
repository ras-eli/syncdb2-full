package osplus.syncdb2.spring;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import osplus.syncdb2.core.domain.SyncEnvelope;
import osplus.syncdb2.core.util.EnvelopeFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zweck
 * -----
 * Integrations-Test: Erfolgsfall. Mehrere record()-Aufrufe in einer Spring-
 * Transaktion werden vor Commit via gleicher Connection in die OUTBOX geschrieben.
 * Zusätzlich wird eine BUSINESS-Tabelle geschrieben, um die Atomizität zu veranschaulichen.
 */
public class SpringTxOutboxSuccessIT {

    static DataSource ds;
    static DataSourceTransactionManager txm;

    @BeforeAll
    static void setup() throws Exception {
        org.h2.jdbcx.JdbcDataSource h2 = new org.h2.jdbcx.JdbcDataSource();
        h2.setURL("jdbc:h2:mem:txoutbox_ok;DB_CLOSE_DELAY=-1");
        ds = h2;
        txm = new DataSourceTransactionManager(ds);
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE OUTBOX (id IDENTITY PRIMARY KEY, envelope_json CLOB, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE BUSINESS (id IDENTITY PRIMARY KEY, note VARCHAR(64))");
        }
    }

    @Test
    @DisplayName("Alle Envelopes landen vor Commit in OUTBOX; BUSINESS-Insert wird mit-committet")
    void commit_writes_outbox_and_business() throws Exception {
        var port = new H2SendPort();
        var def = new DefaultTransactionDefinition();
        TransactionStatus status = txm.getTransaction(def);

        // Fachliche Änderung (Business)
        try (PreparedStatement ps = ds.getConnection().prepareStatement("INSERT INTO BUSINESS(note) VALUES ('ok')")) {
            ps.executeUpdate();
        }
        SpringTxOutbox.setDefaults(ds, port);

        // Mehrere Envelopes registrieren
        SpringTxOutbox.record(
                EnvelopeFactory.create("C", "A", Map.of("k", 1), "v1", "corr", null, null)
        );
        SpringTxOutbox.record(
                EnvelopeFactory.create("C", "B", Map.of("k", 2), "v1", "corr", null, null)
        );

        txm.commit(status);

        try (var rs = ds.getConnection().createStatement().executeQuery("SELECT COUNT(*) FROM OUTBOX")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(2);
        }
        try (var rs = ds.getConnection().createStatement().executeQuery("SELECT COUNT(*) FROM BUSINESS")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }

    static class H2SendPort implements MqSendPort {
        @Override
        public void sendWithinTx(Connection connection, SyncEnvelope envelope) throws SQLException {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO OUTBOX(envelope_json) VALUES (?)")) {
                ps.setString(1, osplus.syncdb2.core.util.EnvelopeJson.toCanonicalJson(envelope));
                ps.executeUpdate();
            }
        }
    }
}
