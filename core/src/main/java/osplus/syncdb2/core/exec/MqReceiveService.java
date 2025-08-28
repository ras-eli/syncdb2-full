package osplus.syncdb2.core.exec;

/**
 * Zweck
 * -----
 * Kapselt das **asynchrone** Abholen/Verarbeiten von Envelopes (Replay) auf
 * der Secondary-DB (z. B. via Stored Procedure `readFromMq`). Die Ausführung
 * geschieht **außerhalb** der Primary-Transaktion.
 * <p>
 * Hinweise
 * --------
 * - Enthält in der Implementierung Idempotenzkontrollen und Fehler-/Retry-Strategien.
 * - Batch- und Poll-Intervalle werden über Konfiguration gesteuert.
 */
public interface MqReceiveService {

    /**
     * Führt genau einen Poll-/Verarbeitungszyklus aus (z. B. vom Scheduler aufgerufen).
     */
    void pollOnce();
}
