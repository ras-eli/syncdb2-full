package osplus.syncdb2.sample.exec;

import org.slf4j.MDC;
import osplus.syncdb2.core.domain.SyncEnvelope;
import osplus.syncdb2.core.exec.MqSendService;
import osplus.syncdb2.core.exec.OrchestrationContext;
import osplus.syncdb2.core.exec.OrchestrationService;
import osplus.syncdb2.core.exec.SendToMqRepository;
import osplus.syncdb2.core.exec.impl.MqSendServiceImpl;
import osplus.syncdb2.core.logging.MdcKeys;
import osplus.syncdb2.core.util.EnvelopeFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Function;

/**
 * Zweck
 * -----
 * Einfache, unabhängige Orchestrierungs-Implementierung für das Sample (ohne Spring).
 * Öffnet eine Primary-Transaktion (AutoCommit=false) und stellt einen imperativen
 * Kontext bereit. Jeder erfolgreiche SQLJ-Aufruf erzeugt ein Envelope und ruft
 * das Send-Repository auf (Transactional-Outbox-Semantik).
 */
public class SimpleOrchestrationService implements OrchestrationService {

    private final DataSource primaryDataSource;
    private final SendToMqRepository sendRepo;

    public SimpleOrchestrationService(DataSource primaryDataSource, SendToMqRepository sendRepo) {
        this.primaryDataSource = primaryDataSource;
        this.sendRepo = sendRepo;
    }

    @Override
    public <R> R callInPrimaryTx(String correlationId, Function<OrchestrationContext, R> flow) {
        try (Connection c = primaryDataSource.getConnection()) {
            boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                OrchestrationContext ctx = new SimpleContext(correlationId, c, sendRepo);
                R result = flow.apply(ctx);
                c.commit();
                return result;
            } catch (Exception e) {
                c.rollback();
                throw new RuntimeException("Tx rollback: " + e.getMessage(), e);
            } finally {
                c.setAutoCommit(prev);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static class SimpleContext implements OrchestrationContext {
        private final String correlationId;
        private final Connection connection;
        private final MqSendService mqSendService;

        SimpleContext(String correlationId, Connection connection, SendToMqRepository repo) {
            this.correlationId = correlationId;
            this.connection = connection;
            this.mqSendService = new MqSendServiceImpl(repo, connection);
        }

        @Override
        public <T> T executeSqlj(String sqljClassName, String sqljMethodName, Map<String, Object> params) {
            return executeSqlj(sqljClassName, sqljMethodName, params, false);
        }

        @Override
        public <T> T executeSqlj(String sqljClassName, String sqljMethodName, Map<String, Object> params, boolean disableAutoSend) {
            try {
                // In diesem Sample simulieren wir die SQLJ-Execution NICHT real; Adapter werden im Scenario genutzt.
                // Hier kümmern wir uns nur um das automatische Senden bei Erfolg.
                if (!disableAutoSend) {
                    SyncEnvelope env = EnvelopeFactory.create(sqljClassName, sqljMethodName, params, "v1", correlationId, null, null);
                    MDC.put(MdcKeys.CORRELATIONID, correlationId);
                    mqSendService.send(env);
                    MDC.remove(MdcKeys.CORRELATIONID);
                }
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void sendToMqManually(SyncEnvelope envelope) {
            mqSendService.send(envelope);
        }
    }
}
