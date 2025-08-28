package osplus.syncdb2.core.exec.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import osplus.syncdb2.core.domain.SyncEnvelope;
import osplus.syncdb2.core.exec.MqReceiveService;
import osplus.syncdb2.core.exec.ProcessedMessageRepository;
import osplus.syncdb2.core.exec.ReadFromMqRepository;
import osplus.syncdb2.core.exec.SecondarySqljExecutor;
import osplus.syncdb2.core.logging.MdcKeys;
import osplus.syncdb2.core.util.EnvelopeParser;

import java.sql.Connection;
import java.util.List;

/**
 * Zweck
 * -----
 * Standard-Implementierung von {@link MqReceiveService}. Holt per `READ_FROM_MQ`
 * einen Batch von Envelopes, parst das JSON-Array, führt jeden Eintrag idempotent
 * auf der Secondary-DB aus und markiert ihn als verarbeitet.
 * <p>
 * Hinweise
 * --------
 * - Läuft **außerhalb** der Primary-Transaktion.
 * - Fehler bei einzelnen Envelopes werden geloggt; die Verarbeitung der übrigen
 * Envelopes wird fortgesetzt (At-least-once).
 */
public class MqReceiveServiceImpl implements MqReceiveService {

    private static final Logger log = LoggerFactory.getLogger(MqReceiveServiceImpl.class);

    private final ReadFromMqRepository repository;
    private final ProcessedMessageRepository processedRepo;
    private final SecondarySqljExecutor secondaryExecutor;
    private final Connection secondaryConnection;
    private final int maxBatch;
    private final ObjectMapper mapper;
    private final EnvelopeParser envelopeParser;

    public MqReceiveServiceImpl(ReadFromMqRepository repository,
                                ProcessedMessageRepository processedRepo,
                                SecondarySqljExecutor secondaryExecutor,
                                Connection secondaryConnection,
                                int maxBatch, ObjectMapper mapper) {
        this.repository = repository;
        this.processedRepo = processedRepo;
        this.secondaryExecutor = secondaryExecutor;
        this.secondaryConnection = secondaryConnection;
        this.maxBatch = maxBatch;
        this.mapper = mapper;
        this.envelopeParser = new EnvelopeParser(mapper);
    }

    @Override
    public void pollOnce() {
        log.info("READ_FROM_MQ: Starte Poll (maxBatch={})", maxBatch);
        try {
            ReadFromMqRepository.Result r = repository.callReadFromMq(secondaryConnection, maxBatch);
            if (r.status() != 0) {
                log.warn("READ_FROM_MQ lieferte Fehlerstatus (status={}, msg={})", r.status(), r.errorMessage());
                return;
            }
            List<SyncEnvelope> list = envelopeParser.fromJsonArray(r.envelopesJson());
            log.info("READ_FROM_MQ: {} Envelopes empfangen", list.size());
            for (SyncEnvelope env : list) {
                MDC.put(MdcKeys.MESSAGE_ID, env.messageId());
                MDC.put(MdcKeys.SQLJ_CLASS, env.sqljClassName());
                MDC.put(MdcKeys.SQLJ_METHOD, env.sqljMethodName());
                try {
                    if (processedRepo.exists(secondaryConnection, env.messageId())) {
                        log.debug("Envelope bereits verarbeitet, überspringe (messageId={})", env.messageId());
                        continue;
                    }
                    secondaryExecutor.execute(env);
                    processedRepo.markProcessed(secondaryConnection, env.messageId());
                    log.info("Envelope verarbeitet (messageId={})", env.messageId());
                } catch (Exception ex) {
                    log.error("Fehler bei Verarbeitung von messageId={}: {}", env.messageId(), ex.getMessage(), ex);
                    // continue with next envelope
                } finally {
                    MDC.remove(MdcKeys.MESSAGE_ID);
                    MDC.remove(MdcKeys.SQLJ_CLASS);
                    MDC.remove(MdcKeys.SQLJ_METHOD);
                }
            }
        } catch (Exception e) {
            log.error("READ_FROM_MQ: Technischer Fehler: {}", e.getMessage(), e);
        } finally {
            log.info("READ_FROM_MQ: Poll Ende");
        }
    }
}
