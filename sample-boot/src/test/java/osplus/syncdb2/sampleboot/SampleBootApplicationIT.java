package osplus.syncdb2.sampleboot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import osplus.syncdb2.core.util.EnvelopeJson;
import osplus.syncdb2.spring.SpringTxOutbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SampleBootApplicationIT {

    @Autowired
    DemoFlowService service;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void happyPath_commit_persists_orders_and_outbox() throws Exception {
        service.runHappyPath();

        Integer orders = jdbc.queryForObject("SELECT COUNT(*) FROM ORDERS", Integer.class);
        Integer outbox = jdbc.queryForObject("SELECT COUNT(*) FROM OUTBOX", Integer.class);
        Integer audit = jdbc.queryForObject("SELECT COUNT(*) FROM AUDIT", Integer.class);

        assertThat(orders).isEqualTo(2);
        assertThat(audit).isEqualTo(1);
        assertThat(outbox).isGreaterThanOrEqualTo(2);
    }

    @Test
    void failure_in_beforeCommit_rollbacks_everything() throws Exception {
        // Port را به شکلی تنظیم می‌کنیم که اگر note == 'FAIL' باشد، استثناء بیندازد.
        SpringTxOutbox.setDefaults(
                jdbc.getDataSource(),
                (conn, env) -> {
                    String json = EnvelopeJson.toCanonicalJson(env);
                    if (json.contains("\"note\":\"FAIL\"")) throw new java.sql.SQLException("boom");
                    try (var ps = conn.prepareStatement("INSERT INTO OUTBOX(envelope_json) VALUES (?)")) {
                        ps.setString(1, json);
                        ps.executeUpdate();
                    }
                }
        );

        try {
            service.runWithFailureOnSecond();
            fail("Expected exception");
        } catch (Exception expected) {
            // ignore
        }

        Integer orders = jdbc.queryForObject("SELECT COUNT(*) FROM ORDERS", Integer.class);
        Integer outbox = jdbc.queryForObject("SELECT COUNT(*) FROM OUTBOX", Integer.class);
        Integer audit = jdbc.queryForObject("SELECT COUNT(*) FROM AUDIT", Integer.class);

        // هیچ چیز نباید اضافه شده باشد
        assertThat(orders).isEqualTo(0);
        assertThat(outbox).isEqualTo(0);
        assertThat(audit).isEqualTo(0);
    }
}
