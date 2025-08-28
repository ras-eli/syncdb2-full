package osplus.syncdb2.core.exec;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Zweck
 * -----
 * Kapselt den **SQLJ-Aufruf** der Stored Procedure `SEND_TO_MQ`.
 * Im Gegensatz zur früheren JDBC-Variante erfolgt der Call jetzt **über eine SQLJ-Klasse**.
 *
 * Erwartete SQLJ-Integration
 * --------------------------
 * - Eine vom SQLJ-Compiler generierte Klasse (z. B. {@code SendToMq_Sqlj}) stellt
 *   eine statische Methode bereit, die die Prozedur aufruft.
 *   Beispielsignatur:
 *   <pre>
 *     SendToMq_Sqlj.Result callSendToMq(Connection conn, String envelopeJson)
 *   </pre>
 * - Das konkrete Klassen-/Methoden-API hängt von Ihrem SQLJ-Setup ab.
 *   Diese Repository-Klasse delegiert nur; sie führt **keinen eigenen JDBC-Code** mehr aus.
 *
 * Testbarkeit
 * -----------
 * - Über das Interface {@link SendToMqSqlj} kann im Test ein Stub/Mock injiziert werden,
 *   sodass die Tests ohne echtes SQLJ-Runtime/DB laufen.
 *
 * Transaktion
 * -----------
 * - Diese Methode führt **kein** COMMIT/ROLLBACK aus; sie läuft innerhalb
 *   der bestehenden Spring-Transaktion (Connection wird von außen übergeben).
 */
public class SendToMqRepository {

    /** SQLJ-Delegate (echte SQLJ-Klasse in Prod, Mock im Test). */
    private final SendToMqSqlj sqlj;

    /** Standard-Konstruktor: verwendet die Default-Delegation auf die generierte SQLJ-Klasse. */
    public SendToMqRepository() {
        this(new DefaultSqlj());
    }

    /** Erlaubt explizite Injektion (z. B. für Tests). */
    public SendToMqRepository(SendToMqSqlj sqlj) {
        this.sqlj = sqlj;
    }

    /**
     * Führt die Prozedur via SQLJ aus.
     *
     * @param connection   von Spring gebundene TX-Connection (gleiche Transaktion)
     * @param envelopeJson JSON-Payload des Envelopes
     * @return Ergebnisobjekt (Status, Fehlertext)
     * @throws SQLException Weiterreichen von DB-/SQLJ-Fehlern (Rollback steuert Spring)
     */
    public Result callSendToMq(Connection connection, String envelopeJson) throws SQLException {
        SendToMqSqlj.Result r = sqlj.callSendToMq(connection, envelopeJson);
        return new Result(r.status(), r.errorMessage());
    }


    /**
     * Default-Implementierung, die **direkt an die generierte SQLJ-Klasse** delegiert.
     * Passen Sie Klassennamen/Methodenaufruf an Ihr konkretes SQLJ-API an.
     */
    static final class DefaultSqlj implements SendToMqSqlj {
        @Override
        public Result callSendToMq(Connection connection, String envelopeJson) throws SQLException {
            // ------------------------------------------------------------
            // >>>> WICHTIG: Hier an Ihre generierte SQLJ-Klasse anpassen <<<<
            // Beispiel: Wenn der SQLJ-Compiler eine Klasse "SendToMq_Sqlj" mit der Methode
            //           "callSendToMq(Connection, String)" erzeugt und ein Ergebnisobjekt
            //           mit den Accessoren getStatus()/getErrorMessage() liefert:
            //
            //   var r = SendToMq_Sqlj.callSendToMq(connection, envelopeJson);
            //   return new Result(r.getStatus(), r.getErrorMessage());
            //
            // Unten ist die Version mit direkten Methoden (status()/errorMessage()) gezeigt.
            // ------------------------------------------------------------
            // var r = SendToMq_Sqlj.callSendToMq(connection, envelopeJson); // <-- Ihre SQLJ-Klasse
            // Wenn Ihr Result anders heißt/gebaut ist, bitte hier mappen:
            int status = 0 ;//r.getStatus();            // oder r.status()
            String err = "";//r.getErrorMessage();      // oder r.errorMessage()
            return new Result(status, err);
        }
    }

    /** Kleines, unveränderliches Ergebnisobjekt für Aufrufer dieses Repositories. */
    public record Result(int status, String errorMessage) {}
}