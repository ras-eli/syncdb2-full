package osplus.syncdb2.core.exec;

import java.util.function.Function;

/**
 * Zweck
 * -----
 * Öffnet eine **Primary-Transaktion** (DB2) und führt einen vom Entwickler
 * gelieferten **imperativen Flow** aus. Innerhalb dieses Flows können beliebige
 * Prüfungen/Service-Aufrufe stattfinden sowie SQLJ-Operationen über den
 * bereitgestellten {@link OrchestrationContext}.
 * <p>
 * Verwendung
 * ----------
 * <pre>
 * OrchestrationService svc = ...;
 * String result = svc.callInPrimaryTx("corr-123", ctx -> {
 *   ctx.executeSqlj("com.company.sqlj.OrderSqlj", "insertOrder",
 *       java.util.Map.of("orderId", 123L, "amount", new java.math.BigDecimal("42.50")));
 *   return "ok";
 * });
 * </pre>
 * <p>
 * Hinweise
 * --------
 * - Die konkrete Transaktionsverwaltung (z. B. @Transactional("primaryTransactionManager"))
 * erfolgt in der Implementierung dieser Schnittstelle.
 * - Fehler innerhalb des Flows führen zum **Rollback** der gesamten Transaktion.
 */
public interface OrchestrationService {

    /**
     * Führt den angegebenen Flow innerhalb einer Primary-Transaktion aus.
     *
     * @param correlationId Korrelation zur Nachverfolgung im Logging (MDC)
     * @param flow          Imperativer Ablauf, der den {@link OrchestrationContext} nutzt
     * @param <R>           Rückgabewert des Flows
     * @return Ergebnis des Flows
     */
    <R> R callInPrimaryTx(String correlationId, Function<OrchestrationContext, R> flow);
}
