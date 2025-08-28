package osplus.syncdb2.spring.boot;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import osplus.syncdb2.core.exec.SendToMqRepository;
import osplus.syncdb2.core.exec.SqljAdapter;
import osplus.syncdb2.core.exec.SqljRegistry;
import osplus.syncdb2.core.exec.impl.InMemorySqljRegistry;
import osplus.syncdb2.spring.MqSendPort;
import osplus.syncdb2.spring.SendToMqPortImpl;
import osplus.syncdb2.spring.SpringTxOutbox;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Zweck
 * -----
 * Auto-Konfiguration für syncdb2 in Spring Boot Anwendungen.
 * Stellt Standard-Beans bereit (Registry, MqSendPort) und setzt
 * Defaults für {@link SpringTxOutbox}, sodass Services sofort
 * SQLJ + Outbox innerhalb einer @Transactional-Methode nutzen können.
 * <p>
 * Hinweise
 * --------
 * - Alle Beans sind mit ConditionalOnMissingBean versehen und können
 * vom Anwender überschrieben werden.
 * - Registry wird automatisch mit allen vorhandenen SqljAdapter-Beans
 * aus dem ApplicationContext befüllt.
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(Syncdb2Properties.class)
@ConditionalOnClass({DataSource.class, SpringTxOutbox.class})
public class Syncdb2AutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SqljRegistry.class)
    public SqljRegistry sqljRegistry(ApplicationContext ctx) {
        InMemorySqljRegistry reg = new InMemorySqljRegistry();
        Map<String, SqljAdapter> adapters = ctx.getBeansOfType(SqljAdapter.class);
        adapters.values().forEach(reg::register);
        return reg;
    }

    @Bean
    @ConditionalOnMissingBean(MqSendPort.class)
    public MqSendPort mqSendPort(SendToMqRepository repo) {
        return new SendToMqPortImpl(repo);
    }

    /**
     * Setzt Default-DS/Port für SpringTxOutbox, falls vorhanden.
     * Wird nach Erstellung der abhängigen Beans ausgeführt.
     */
    @Bean
    @DependsOn({"mqSendPort"})
    public Object syncdb2DefaultsInitializer(ApplicationContext ctx) {
        try {
            DataSource ds = ctx.getBean(DataSource.class); // Primary DataSource
            MqSendPort port = ctx.getBean(MqSendPort.class);
            SpringTxOutbox.setDefaults(ds, port);
        } catch (Exception ignored) {
            // Kein DataSource/MqSendPort im Kontext => nichts zu tun
        }
        return new Object();
    }
}
