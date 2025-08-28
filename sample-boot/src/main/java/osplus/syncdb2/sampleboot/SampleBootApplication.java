package osplus.syncdb2.sampleboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Demo-Anwendung (Spring Boot), die zeigt:
 * - SQLJ-Ausführung innerhalb derselben Spring-Transaktion
 * - Outbox (vor Commit) via SpringTxOutbox
 * H2 wird automatisch konfiguriert; Schema wird aus schema.sql geladen.
 */
@SpringBootApplication
public class SampleBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(SampleBootApplication.class, args);
    }
}
