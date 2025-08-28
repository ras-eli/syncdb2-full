package osplus.syncdb2.core.exec;
// ---------------------------------------------------------------------------
// Abstraktion über die SQLJ-Schicht (für Prod = echte SQLJ-Klasse; für Tests = Mock)
// ---------------------------------------------------------------------------

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstraktes SQLJ-API, das von der generierten Klasse abgebildet wird.
 * Production-Code sollte eine Implementierung verwenden, die an die echte SQLJ-Klasse delegiert.
 */
public interface SendToMqSqlj {
    /** Minimales Ergebnisobjekt aus der Prozedur. */
    record Result(int status, String errorMessage) {}
    /** Führt den SQLJ-Call aus. */
    Result callSendToMq(Connection connection, String envelopeJson) throws SQLException;
}
