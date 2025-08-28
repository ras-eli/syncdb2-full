package osplus.syncdb2.spring;

import osplus.syncdb2.core.domain.SyncEnvelope;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Zweck
 * -----
 * Port/Abstraktion für das Senden eines {@link SyncEnvelope} **innerhalb der
 * aktuellen DB-Transaktion**. Die Methode erhält die bereits von Spring
 * gebundene, transaktionale {@link Connection}.
 * <p>
 * Warum ein Port?
 * ---------------
 * - Entkoppelt die Spring-Tx-Orchestrierung von der konkreten Sende-Implementierung
 * (z. B. Aufruf eines Stored Procedures via Repository).
 * - Erleichtert das Testen (Mock/Stub des Ports).
 */
public interface MqSendPort {
    void sendWithinTx(Connection connection, SyncEnvelope envelope) throws SQLException;
}
