package osplus.syncdb2.spring;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import osplus.syncdb2.core.exec.SqljAdapter;
import osplus.syncdb2.core.exec.SqljRegistry;
import osplus.syncdb2.core.exec.impl.InMemorySqljRegistry;

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
 * Integrations-Test: SQLJ-Ausführung **innerhalb derselben Spring-TX**
 * über {@link SqljExecutorSpring}. Verifiziert wird, dass:
 * - beide SQLJ-Aufrufe in einer TX laufen (Commit persistiert beide Inserts)
 * - Fehler zu Rollback führt.
 */
public class SqljExecutorSpringIT {

    static DataSource ds;
    static DataSourceTransactionManager txm;
    static SqljRegistry registry;

    @BeforeAll
    static void init() throws Exception {
        org.h2.jdbcx.JdbcDataSource h2 = new org.h2.jdbcx.JdbcDataSource();
        h2.setURL("jdbc:h2:mem:sqljexec;DB_CLOSE_DELAY=-1");
        ds = h2;
        txm = new DataSourceTransactionManager(ds);
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE ORDERS (id IDENTITY PRIMARY KEY, note VARCHAR(100))");
        }
        // Registry + Adapter, der auf der gegebenen Connection schreibt
        registry = new InMemorySqljRegistry();
        registry.register(new SqljAdapter<Integer>() {
            @Override
            public String sqljClassName() {
                return "C";
            }

            @Override
            public String sqljMethodName() {
                return "ins";
            }

            @Override
            public Integer execute(Connection connection, Map<String, Object> params) throws SQLException {
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO ORDERS(note) VALUES (?)")) {
                    ps.setString(1, String.valueOf(params.get("n")));
                    return ps.executeUpdate();
                }
            }
        });
    }

    @Test
    @DisplayName("Zwei SQLJ-Aufrufe in einer TX → Commit persistiert beide Inserts")
    void two_calls_commit() throws Exception {
        var def = new DefaultTransactionDefinition();
        TransactionStatus status = txm.getTransaction(def);

        SqljExecutorSpring.execute(registry, ds, "C", "ins", Map.of("n", "A"));
        SqljExecutorSpring.execute(registry, ds, "C", "ins", Map.of("n", "B"));

        txm.commit(status);

        try (var rs = ds.getConnection().createStatement().executeQuery("SELECT COUNT(*) FROM ORDERS")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Fehler im zweiten Aufruf → Rollback; keine neuen Zeilen")
    void failure_rollback() throws Exception {
        // Adapter überschreiben, der beim zweiten Eintrag exception wirft
        registry = new InMemorySqljRegistry();
        registry.register(new SqljAdapter<Integer>() {
            @Override
            public String sqljClassName() {
                return "C";
            }

            @Override
            public String sqljMethodName() {
                return "ins";
            }

            @Override
            public Integer execute(Connection connection, Map<String, Object> params) throws SQLException {
                String note = String.valueOf(params.get("n"));
                if ("X".equals(note)) throw new SQLException("boom");
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO ORDERS(note) VALUES (?)")) {
                    ps.setString(1, note);
                    return ps.executeUpdate();
                }
            }
        });

        var def = new DefaultTransactionDefinition();
        TransactionStatus status = txm.getTransaction(def);

        assertThatThrownBy(() -> {
            try {
                SqljExecutorSpring.execute(registry, ds, "C", "ins", Map.of("n", "OK"));
                SqljExecutorSpring.execute(registry, ds, "C", "ins", Map.of("n", "X"));
                txm.commit(status);
            } catch (Exception e) {
                txm.rollback(status);
                throw e;
            }
        }).isInstanceOf(SQLException.class);

        try (var rs = ds.getConnection().createStatement().executeQuery("SELECT COUNT(*) FROM ORDERS")) {
            rs.next();
            // bleibt 2 aus dem vorherigen Test
            assertThat(rs.getInt(1)).isEqualTo(2);
        }
    }
}
