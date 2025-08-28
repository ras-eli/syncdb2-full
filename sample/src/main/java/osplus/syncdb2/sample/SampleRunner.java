package syncdb2.sample;

import osplus.syncdb2.sample.config.DataSources;
import osplus.syncdb2.sample.exec.SimpleOrchestrationService;
import osplus.syncdb2.sample.repo.SendToMqRepositoryH2;
import osplus.syncdb2.sample.scenarios.AccountSetupScenario;
import osplus.syncdb2.sample.scenarios.InstallmentPaymentScenario;
import osplus.syncdb2.sample.scenarios.PurchaseScenario;

import javax.sql.DataSource;

/**
 * Zweck
 * -----
 * Einfache Main-Klasse, die die drei Beispiel-Szenarien nacheinander ausführt.
 * Dient ausschließlich der Demonstration; in realen Projekten würde Spring Boot
 * die Beans/Services bereitstellen.
 */
public class SampleRunner {

    public static void main(String[] args) throws Exception {
        DataSource primary = DataSources.primary();
        DataSource secondary = DataSources.secondary();
        DataSources.initSchema(primary);
        DataSources.initSchema(secondary);

        var sendRepo = new SendToMqRepositoryH2();
        var orchestration = new SimpleOrchestrationService(primary, sendRepo);

        System.out.println("Purchase: " + PurchaseScenario.run(orchestration));
        System.out.println("Installment: " + InstallmentPaymentScenario.run(orchestration));
        System.out.println("AccountSetup: " + AccountSetupScenario.run(orchestration));

        System.out.println("Sample completed.");
    }
}
