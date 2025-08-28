package osplus.syncdb2.sample.scenarios;

import osplus.syncdb2.core.exec.OrchestrationContext;
import osplus.syncdb2.core.exec.OrchestrationService;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Zweck
 * -----
 * Szenario 1: Purchase. Führt zwei SQLJ-Operationen aus:
 * 1) Order anlegen
 * 2) Lagerbestand aktualisieren
 * <p>
 * Nach jedem erfolgreichen Schritt wird automatisch ein Envelope erzeugt
 * und via SEND_TO_MQ geloggt (in der Primary-Transaktion).
 */
public final class PurchaseScenario {

    private PurchaseScenario() {
    }

    public static String run(OrchestrationService service) {
        return service.callInPrimaryTx("corr-purchase", (OrchestrationContext ctx) -> {
            ctx.executeSqlj("com.company.sqlj.OrderSqlj", "insertOrder",
                    Map.of("orderId", 1001L, "customerId", 9001L, "amount", new BigDecimal("42.50")));
            ctx.executeSqlj("com.company.sqlj.InventorySqlj", "updateStock",
                    Map.of("productId", 77L, "quantityDelta", -1));
            return "OK";
        });
    }
}
