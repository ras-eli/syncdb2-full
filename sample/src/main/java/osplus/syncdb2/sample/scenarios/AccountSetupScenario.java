package osplus.syncdb2.sample.scenarios;

import osplus.syncdb2.core.exec.OrchestrationContext;
import osplus.syncdb2.core.exec.OrchestrationService;

import java.util.Map;

/**
 * Zweck
 * -----
 * Szenario 3: Account Setup. Führt zwei SQLJ-Operationen aus:
 * 1) Konto anlegen
 * 2) Limits zuweisen
 */
public final class AccountSetupScenario {

    private AccountSetupScenario() {
    }

    public static String run(OrchestrationService service) {
        return service.callInPrimaryTx("corr-account", (OrchestrationContext ctx) -> {
            ctx.executeSqlj("com.company.sqlj.AccountSqlj", "createAccount",
                    Map.of("customerId", 7001L, "currency", "EUR"));
            ctx.executeSqlj("com.company.sqlj.LimitSqlj", "assignLimits",
                    Map.of("accountId", 222L, "dailyLimit", 1000));
            return "OK";
        });
    }
}
