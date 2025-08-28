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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Zweck
 * -----
 * Integrations-Test: Fehlerfall. Der Port wirft beim Senden eine Exception.
 * Erwartung: Commit wirft RuntimeException und die gesamte Transaktion wird
 * zurückgerollt (keine Einträge in OUTBOX und BUSINESS).
 */
public class SpringTxOutboxFailureRollbackIT {

    static DataSource ds;
    static DataSourceTransactionManager txm;

    @BeforeAll
    static void setup() throws Exception {
        org.h2.jdbcx.JdbcDataSource h2 = new org.h2.jdbcx.JdbcDataSource();
        h2.setURL("jdbc:h2:mem:txoutbox_fail;DB_CLOSE_DELAY=-1");
        ds = h2;
        txm = new DataSourceTransactionManager(ds);
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE OUTBOX (id IDENTITY PRIMARY KEY, envelope_json CLOB, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE BUSINESS (id IDENTITY PRIMARY KEY, note VARCHAR(64))");
        }
    }

    @Test
    @DisplayName("Fehler beim Outbox-Senden -> RuntimeException -> Rollback von OUTBOX und BUSINESS")
    void rollback_on_failure() throws Exception {
        var port = new FailingPort();
        var def = new DefaultTransactionDefinition();
        TransactionStatus status = txm.getTransaction(def);

        // Fachliche Änderung (Business)
        try (PreparedStatement ps = ds.getConnection().prepareStatement("INSERT INTO BUSINESS(note) VALUES ('should_rollback')")) {
            ps.executeUpdate();
        }

        // Ein Envelope registrieren
        SpringTxOutbox.setDefaults(ds, port);
        SpringTxOutbox.record(
                EnvelopeFactory.create("C", "X", Map.of("k", 1), "v1", "corr", null, null)
        );

        assertThatThrownBy(() -> txm.commit(status))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Outbox-Sendung vor Commit fehlgeschlagen");

        try (var rs = ds.getConnection().createStatement().executeQuery("SELECT COUNT(*) FROM OUTBOX")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(0);
        }
        try (var rs = ds.getConnection().createStatement().executeQuery("SELECT COUNT(*) FROM BUSINESS")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(0);
        }
    }

    static class FailingPort implements MqSendPort {
        @Override
        public void sendWithinTx(Connection connection, SyncEnvelope envelope) throws SQLException {
            throw new SQLException("boom");
        }
    }
}
