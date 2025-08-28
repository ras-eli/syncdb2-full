package osplus.syncdb2.core.exec;

import osplus.syncdb2.core.domain.SyncEnvelope;

/**
 * Zweck
 * -----
 * Kapselt den **synchronen** Aufruf der Stored Procedure/SQLJ-Operation `sendToMq`
 * innerhalb der **Primary-Transaktion**. Dient zur Transactional-Outbox-Semantik:
 * Entweder werden Primary-DB-Änderungen **und** das Logging (Envelope) gemeinsam
 * committet, oder beides wird zurückgerollt.
 * <p>
 * Hinweise
 * --------
 * - Konkrete SP-Signatur und Fehlerbilder werden in einem späteren Schritt festgelegt.
 * - Fehler führen zum **Rollback** der umgebenden Transaktion.
 */
public interface MqSendService {

    /**
     * Persistiert/übermittelt das Envelope synchron (z. B. via Stored Procedure).
     *
     * @param envelope Replay-Payload (deterministisch, JSON-serialisierbar)
     */
    void send(SyncEnvelope envelope);
}
