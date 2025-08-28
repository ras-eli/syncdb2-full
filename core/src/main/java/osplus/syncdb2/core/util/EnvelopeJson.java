package osplus.syncdb2.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import osplus.syncdb2.core.domain.SyncEnvelope;

import java.util.Map;

/**
 * Zweck
 * -----
 * Erzeugt **kanonisches JSON** für {@link SyncEnvelope} mit stabiler
 * Schlüsselsortierung. Diese Darstellung dient sowohl der Persistenz (sendToMq)
 * als auch der Hash-Bildung (messageId) und muss daher deterministisch sein.
 * <p>
 * Hinweise
 * --------
 * - Verwendet Jackson mit aktivierter Sortierung der Map-Einträge.
 * - Datums-/Zeitwerte werden als ISO-Strings geschrieben (keine Timestamps).
 * - Felder, die nicht im Hash berücksichtigt werden sollen (z. B. createdAt),
 * dürfen für die Hash-Bildung separat serialisiert werden.
 */
public final class EnvelopeJson {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .addModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

            .build();

    private EnvelopeJson() {
    }

    /**
     * Serialisiert das gesamte Envelope als kanonisches JSON.
     */
    public static String toCanonicalJson(SyncEnvelope env) {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("messageId", env.messageId());
            root.put("sqljClassName", env.sqljClassName());
            root.put("sqljMethodName", env.sqljMethodName());
            root.set("params", MAPPER.valueToTree(env.params()));
            if (env.scenarioId() != null) root.put("scenarioId", env.scenarioId());
            if (env.stepIndex() != null) root.put("stepIndex", env.stepIndex());
            root.put("version", env.version());
            if (env.correlationId() != null) root.put("correlationId", env.correlationId());
            root.put("createdAt", env.createdAt().toString());
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Konnte Envelope nicht in kanonisches JSON serialisieren", e);
        }
    }

    /**
     * Serialisiert nur die stabilen Felder für die Hash-Bildung (ohne createdAt).
     */
    public static String toCanonicalJsonForHash(String sqljClassName, String sqljMethodName, Map<String, Object> params, String version) {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("sqljClassName", sqljClassName);
            root.put("sqljMethodName", sqljMethodName);
            root.set("params", MAPPER.valueToTree(params));
            root.put("version", version);
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Konnte Hash-JSON nicht erzeugen", e);
        }
    }
}
