package osplus.syncdb2.spring;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import osplus.syncdb2.core.domain.SyncEnvelope;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zweck
 * -----
 * **Transaktionsgebundener Outbox-Puffer** für {@link SyncEnvelope}-Nachrichten
 * in Spring-Transaktionen. Envelopes werden während der Geschäftslogik
 * gesammelt und **unmittelbar vor Commit** (d. h. innerhalb derselben
 * DB-Transaktion) versendet.
 * <p>
 * Anwendungsfälle
 * ---------------
 * - Vollständig imperativer Stil ohne zentralen Orchestrator
 * - Mehrere Services, die innerhalb **derselben** Spring-TX arbeiten
 * - Unterstützt mehrere DataSources/TransactionManager (je DataSource separater Puffer)
 * <p>
 * API-Varianten
 * -------------
 * - {@link #record(SyncEnvelope)}            : nutzt Default-DataSource/-Port (muss vorher gesetzt werden)
 * - {@link #record(SyncEnvelope, DataSource)}: nutzt angegebene DataSource, Default-Port
 * - {@link #recordWithTxManager(SyncEnvelope, DataSource, MqSendPort)}: volle Kontrolle
 */
public final class SpringTxOutbox {

    /**
     * Thread-lokaler Puffer: je DataSource eine Liste von Envelopes.
     */
    private static final ThreadLocal<Map<DataSource, List<SyncEnvelope>>> TX_BUFFER =
            ThreadLocal.withInitial(ConcurrentHashMap::new);
    /**
     * Verhindert doppelte Registrierung der Synchronization je DataSource.
     */
    private static final ThreadLocal<Set<DataSource>> SYNC_REGISTERED =
            ThreadLocal.withInitial(HashSet::new);
    /**
     * Default-Objekte (optional), z. B. via Spring @Configuration setzbar.
     */
    private static volatile DataSource DEFAULT_DS;
    private static volatile MqSendPort DEFAULT_PORT;

    private SpringTxOutbox() {
    }

    /**
     * Defaults setzen (z. B. in einer Spring-@Configuration).
     */
    public static void setDefaults(DataSource ds, MqSendPort port) {
        DEFAULT_DS = ds;
        DEFAULT_PORT = port;
    }

    /**
     * Verwendung mit zuvor gesetzten Defaults.
     */
    public static void record(SyncEnvelope envelope) {
        if (DEFAULT_DS == null || DEFAULT_PORT == null) {
            throw new IllegalStateException("Defaults nicht gesetzt: setDefaults(DataSource, MqSendPort) erforderlich.");
        }
        recordWithTxManager(envelope, DEFAULT_DS, DEFAULT_PORT);
    }

    /**
     * Verwendung mit expliziter DataSource und Default-Port.
     */
    public static void record(SyncEnvelope envelope, DataSource dataSource) {
        if (DEFAULT_PORT == null) {
            throw new IllegalStateException("Default-Port nicht gesetzt: setDefaults(DataSource, MqSendPort) erforderlich.");
        }
        recordWithTxManager(envelope, dataSource, DEFAULT_PORT);
    }

    /**
     * Volle Kontrolle: DataSource und Port explizit angeben.
     * Arbeitet **innerhalb der aktuellen Spring-TX**; vor Commit wird gesendet.
     */
    public static void recordWithTxManager(SyncEnvelope envelope, DataSource dataSource, MqSendPort port) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Keine aktive Spring-Transaktion: record() erfordert @Transactional-Kontext.");
        }
        // 1) Envelope in den Tx-Puffer für die gegebene DataSource aufnehmen
        Map<DataSource, List<SyncEnvelope>> map = TX_BUFFER.get();
        List<SyncEnvelope> list = map.computeIfAbsent(dataSource, ds -> Collections.synchronizedList(new ArrayList<>()));
        list.add(envelope);

        // 2) Falls für diese DataSource noch keine Synchronization registriert ist => registrieren
        Set<DataSource> registered = SYNC_REGISTERED.get();
        if (registered.add(dataSource)) {
            TransactionSynchronizationManager.registerSynchronization(new TxSync(dataSource, port));
        }
    }

    /**
     * TransactionSynchronization, die unmittelbar vor Commit die Outbox sendet.
     */
    private static final class TxSync implements TransactionSynchronization {
        private final DataSource ds;
        private final MqSendPort port;

        TxSync(DataSource ds, MqSendPort port) {
            this.ds = ds;
            this.port = port;
        }

        @Override
        public void beforeCommit(boolean readOnly) {
            Connection c = DataSourceUtils.getConnection(ds); // selbe TX-gebundene Connection
            try {
                List<SyncEnvelope> envelopes = TX_BUFFER.get().getOrDefault(ds, List.of());
                for (SyncEnvelope e : envelopes) {
                    port.sendWithinTx(c, e);
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Outbox-Sendung vor Commit fehlgeschlagen", ex);
            }
        }

        @Override
        public void afterCompletion(int status) {
            Map<DataSource, List<SyncEnvelope>> map = TX_BUFFER.get();
            if (map != null) {
                map.remove(ds);
                Set<DataSource> reg = SYNC_REGISTERED.get();
                if (reg != null) reg.remove(ds);
                if (map.isEmpty()) {
                    TX_BUFFER.remove();
                    SYNC_REGISTERED.remove();
                }
            }
        }
    }
}
