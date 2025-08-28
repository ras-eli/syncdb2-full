package osplus.syncdb2.core.exec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zweck
 * -----
 * Tests für ProcessedMessageRepository. Diese Tests zeigen das erwartete JDBC-Verhalten.
 * In echten Integrations-Tests wird eine H2-DB verwendet.
 */
public class ProcessedMessageRepositoryTest {

    @Test
    @DisplayName("exists: Liefert true wenn Datensatz vorhanden ist")
    void exists_true() throws Exception {
        Connection conn = Mockito.mock(Connection.class);
        PreparedStatement ps = Mockito.mock(PreparedStatement.class);
        ResultSet rs = Mockito.mock(ResultSet.class);
        Mockito.when(conn.prepareStatement(Mockito.anyString())).thenReturn(ps);
        Mockito.when(ps.executeQuery()).thenReturn(rs);
        Mockito.when(rs.next()).thenReturn(true);

        ProcessedMessageRepository repo = new ProcessedMessageRepository();
        boolean ok = repo.exists(conn, "m1");
        assertThat(ok).isTrue();
    }

    @Test
    @DisplayName("markProcessed: Führt INSERT aus (ohne Exception)")
    void markProcessed_insert() throws Exception {
        Connection conn = Mockito.mock(Connection.class);
        PreparedStatement ps = Mockito.mock(PreparedStatement.class);
        Mockito.when(conn.prepareStatement(Mockito.anyString())).thenReturn(ps);
        Mockito.when(ps.executeUpdate()).thenReturn(1);

        ProcessedMessageRepository repo = new ProcessedMessageRepository();
        repo.markProcessed(conn, "m1");
        Mockito.verify(ps, Mockito.times(1)).executeUpdate();
    }
}
