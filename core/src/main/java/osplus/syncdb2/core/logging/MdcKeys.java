package osplus.syncdb2.core.logging;

/**
 * Zweck
 * -----
 * Zentrale Sammlung der MDC-Schlüssel für konsistentes Logging/Tracing.
 */
public final class MdcKeys {

    public static final String SCENARIO_ID = "syncdb2.scenarioId";
    public static final String STEP_INDEX = "syncdb2.stepIndex";
    public static final String CORRELATIONID = "syncdb2.correlationId";
    public static final String MESSAGE_ID = "syncdb2.messageId";
    public static final String SQLJ_CLASS = "syncdb2.sqljClass";
    public static final String SQLJ_METHOD = "syncdb2.sqljMethod";

    private MdcKeys() {
    }
}
