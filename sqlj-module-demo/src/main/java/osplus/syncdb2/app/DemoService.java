package osplus.syncdb2.app;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Deutsch:
 *  Ein einfacher Service, der zeigt, wie man SQLJ-Klassen aufruft.
 *  Die SQLJ-Klassen werden unter osplus.syncdb2.sqlj.* erzeugt/kompiliert.
 */
@Service
public class DemoService {

    private final HikariDataSource dataSource;

    public DemoService(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void demo() throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            // Deutsch: Beispielaufruf (IDs und Werte sind nur Demo)
            new osplus.syncdb2.sqlj.PersonInsert(1, "Alice").execute(con);
            new osplus.syncdb2.sqlj.PersonUpdate(1, "Alicia").execute(con);
            new osplus.syncdb2.sqlj.PersonDelete(1).execute(con);

            new osplus.syncdb2.sqlj.ProductInsert(101, "Phone", new java.math.BigDecimal("399.00")).execute(con);
            new osplus.syncdb2.sqlj.ProductUpdate(101, "Smartphone", new java.math.BigDecimal("449.00")).execute(con);
            new osplus.syncdb2.sqlj.ProductDelete(101).execute(con);
        }
    }
}
