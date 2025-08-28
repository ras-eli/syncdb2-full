package osplus.syncdb2.core.exec.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import osplus.syncdb2.core.domain.SyncEnvelope;
import osplus.syncdb2.core.exception.MqSendException;
import osplus.syncdb2.core.exec.MqSendService;
import osplus.syncdb2.core.exec.SendToMqRepository;
import osplus.syncdb2.core.logging.MdcKeys;
import osplus.syncdb2.core.util.EnvelopeJson;

import java.sql.Connection;

/**
 * Zweck
 * -----
 * Standard-Implementierung von {@link MqSendService}. Serialisiert das Envelope
 * als **kanonisches JSON** und ruft die Stored Procedure `SEND_TO_MQ` über
 * {@link SendToMqRepository} auf. Fehlerhafte Statuscodes führen zu einer
 * {@link MqSendException} und damit zum Rollback der umgebenden Transaktion.
 * <p>
 * Hinweise
 * --------
 * - Diese Klasse verwaltet **keine** Transaktion. Sie wird innerhalb einer
 * bereits geöffneten Primary-Transaktion verwendet (Orchestrator-Ebene).
 * - Die Übergabe der JDBC-Connection kann projektabhängig via ThreadLocal oder
 * über den umgebenden Kontext erfolgen (hier exemplarisch im Konstruktor).
 */
public class MqSendServiceImpl implements MqSendService {

    private static final Logger log = LoggerFactory.getLogger(MqSendServiceImpl.class);

    private final SendToMqRepository repository;
    private final Connection primaryConnection;

    public MqSendServiceImpl(SendToMqRepository repository, Connection primaryConnection) {
        this.repository = repository;
        this.primaryConnection = primaryConnection;
    }

    @Override
    public void send(SyncEnvelope envelope) {
        String messageId = envelope.messageId();
        MDC.put(MdcKeys.MESSAGE_ID, messageId);
        MDC.put(MdcKeys.SQLJ_CLASS, envelope.sqljClassName());
        MDC.put(MdcKeys.SQLJ_METHOD, envelope.sqljMethodName());
        try {
            String json = EnvelopeJson.toCanonicalJson(envelope);
            log.info("Sende Envelope via SEND_TO_MQ (messageId={})", messageId);
            SendToMqRepository.Result r = repository.callSendToMq(primaryConnection, json);
            if (r.status() != 0) {
                log.error("SEND_TO_MQ meldet Fehler (status={}, msg={})", r.status(), r.errorMessage());
                throw new MqSendException(r.status(), r.errorMessage());
            }
            log.info("SEND_TO_MQ erfolgreich (messageId={})", messageId);
        } catch (MqSendException e) {
            throw e; // führt zum Rollback in der umgebenden Tx
        } catch (Exception e) {
            log.error("Fehler beim Aufruf von SEND_TO_MQ (messageId={})", messageId, e);
            throw new MqSendException(-1, "Technischer Fehler bei SEND_TO_MQ: " + e.getMessage());
        } finally {
            MDC.remove(MdcKeys.MESSAGE_ID);
            MDC.remove(MdcKeys.SQLJ_CLASS);
            MDC.remove(MdcKeys.SQLJ_METHOD);
        }
    }
}
