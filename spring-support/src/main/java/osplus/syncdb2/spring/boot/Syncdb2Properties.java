package osplus.syncdb2.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Zweck
 * -----
 * Bindet externe Konfiguration (application.yml/properties) für syncdb2.
 * Hält Einstellungen für MQ-Prozeduren, Polling, Logging etc.
 */
@ConfigurationProperties(prefix = "syncdb2")
public class Syncdb2Properties {

    @NestedConfigurationProperty
    private final Mq mq = new Mq();

    @NestedConfigurationProperty
    private final Logging logging = new Logging();

    public Mq getMq() {
        return mq;
    }

    public Logging getLogging() {
        return logging;
    }

    public static class Mq {
        @NestedConfigurationProperty
        private final Send send = new Send();
        @NestedConfigurationProperty
        private final Read read = new Read();

        public Send getSend() {
            return send;
        }

        public Read getRead() {
            return read;
        }

        public static class Send {
            /**
             * Name der Stored Procedure zum Senden in die Outbox/MQ.
             */
            private String procedureName = "SEND_TO_MQ";

            public String getProcedureName() {
                return procedureName;
            }

            public void setProcedureName(String procedureName) {
                this.procedureName = procedureName;
            }
        }

        public static class Read {
            /**
             * Name der Stored Procedure zum Lesen aus der MQ/Inbox.
             */
            private String procedureName = "READ_FROM_MQ";
            /**
             * Poll-Batch-Größe.
             */
            private int batchSize = 50;
            /**
             * Poll-Intervall in Millisekunden.
             */
            private long fixedDelay = 2000L;

            public String getProcedureName() {
                return procedureName;
            }

            public void setProcedureName(String procedureName) {
                this.procedureName = procedureName;
            }

            public int getBatchSize() {
                return batchSize;
            }

            public void setBatchSize(int batchSize) {
                this.batchSize = batchSize;
            }

            public long getFixedDelay() {
                return fixedDelay;
            }

            public void setFixedDelay(long fixedDelay) {
                this.fixedDelay = fixedDelay;
            }
        }
    }

    public static class Logging {
        /**
         * Parameter-Namen, die im Logging maskiert werden sollen.
         */
        private List<String> maskedParams = new ArrayList<>();

        public List<String> getMaskedParams() {
            return maskedParams;
        }

        public void setMaskedParams(List<String> maskedParams) {
            this.maskedParams = maskedParams;
        }
    }
}
