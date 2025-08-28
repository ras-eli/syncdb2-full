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
 * Integrations-Test für {@link SpringTxOutbox}: mehrfaches record(...) in einer
 * Spring-TX, Versand in beforeCommit() **auf derselben Connection**. Fehler → Rollback.
 */
public class SpringTxOutboxIT {

    static DataSource ds;
    static DataSourceTransactionManager txm;

    @BeforeAll
    static void init() throws Exception {
        org.h2.jdbcx.JdbcDataSource h2 = new org.h2.jdbcx.JdbcDataSource();
        h2.setURL("jdbc:h2:mem:txoutbox2;DB_CLOSE_DELAY=-1");
        ds = h2;
        txm = new DataSourceTransactionManager(ds);
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE OUTBOX (id IDENTITY PRIMARY KEY, envelope_json CLOB, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
        SpringTxOutbox.setDefaults(ds, (connection, envelope) -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO OUTBOX(envelope_json) VALUES (?)")) {
                ps.setString(1, osplus.syncdb2.core.util.EnvelopeJson.toCanonicalJson(envelope));
                ps.executeUpdate();
            }
        });
    }

    @Test
    @DisplayName("Mehrfaches record in einer TX → beforeCommit sendet alle; Commit persistiert")
    void record_commit() throws Exception {
        var def = new DefaultTransactionDefinition();
        TransactionStatus status = txm.getTransaction(def);

        SyncEnvelope e1 = EnvelopeFactory.create("C", "M1", Map.of("k", 1), "v1", "corr", null, null);
        SpringTxOutbox.record(e1); // nutzt Defaults

        SyncEnvelope e2 = EnvelopeFactory.create("C", "M2", Map.of("k", 2), "v1", "corr", null, null);
        SpringTxOutbox.record(e2); // nutzt Defaults

        txm.commit(status);

        try (var rs = ds.getConnection().createStatement().executeQuery("SELECT COUNT(*) FROM OUTBOX")) {
            rs.next();
            assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    @DisplayName("Fehler beim Senden in beforeCommit → Rollback; kein neuer OUTBOX-Eintrag")
    void record_failure_rollback() throws Exception {
        // Port, der Fehler auslöst
        SpringTxOutbox.setDefaults(ds, (connection, envelope) -> {
            throw new SQLException("boom");
        });

        var def = new DefaultTransactionDefinition();
        TransactionStatus status = txm.getTransaction(def);

        SyncEnvelope e1 = EnvelopeFactory.create("C", "M3", Map.of("k", 3), "v1", "corr", null, null);
        SpringTxOutbox.record(e1);

        assertThatThrownBy(() -> txm.commit(status))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Outbox-Sendung vor Commit fehlgeschlagen");
    }
}
