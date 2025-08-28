package osplus.syncdb2.sample.scenarios;

import osplus.syncdb2.core.exec.OrchestrationContext;
import osplus.syncdb2.core.exec.OrchestrationService;

import java.util.Map;

/**
 * Zweck
 * -----
 * Szenario 2: Installment Payment. Führt zwei SQLJ-Operationen aus:
 * 1) Ratenplan anlegen
 * 2) Zahlung anwenden
 */
public final class InstallmentPaymentScenario {

    private InstallmentPaymentScenario() {
    }

    public static String run(OrchestrationService service) {
        return service.callInPrimaryTx("corr-installment", (OrchestrationContext ctx) -> {
            ctx.executeSqlj("com.company.sqlj.InstallmentSqlj", "createInstallment",
                    Map.of("contractId", 5001L, "months", 12, "amountPerMonth", 300));
            ctx.executeSqlj("com.company.sqlj.PaymentSqlj", "applyPayment",
                    Map.of("contractId", 5001L, "paymentAmount", 300));
            return "OK";
        });
    }
}
