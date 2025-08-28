package osplus.syncdb2.core.exec.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import osplus.syncdb2.core.exec.SqljAdapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Zweck
 * -----
 * Umfassende Unit-Tests für {@link InMemorySqljRegistry}:
 * - Registrierung und Lookup
 * - Verhalten bei fehlenden Einträgen
 * - Fail-Fast bei Doppel-Registrierung
 * - Grundsätzliche Thread-Sicherheit bei paralleler Registrierung
 * <p>
 * Hinweise
 * --------
 * - Die Tests verwenden einfache Test-Adapter (lokale Klassen), die nur
 * die Metadaten (sqljClassName/sqljMethodName) bereitstellen.
 * - Die Methode {@code size()} ist paket-sichtbar; Tests befinden sich
 * im selben Package, um Diagnosen zu ermöglichen.
 */
public class InMemorySqljRegistryTest {

    @Test
    @DisplayName("register/find: Erfolgreiche Registrierung und Lookup eines Adapters")
    void register_and_find_success() {
        InMemorySqljRegistry reg = new InMemorySqljRegistry();
        SqljAdapter<Object> a = new TestAdapter("C", "M");
        reg.register(a);

        Optional<SqljAdapter<?>> found = reg.findAdapter("C", "M");
        assertThat(found).isPresent();
        assertThat(found.get()).isSameAs(a);
        assertThat(reg.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("find: Fehlender Eintrag liefert Optional.empty()")
    void find_missing_returns_empty() {
        InMemorySqljRegistry reg = new InMemorySqljRegistry();
        assertThat(reg.findAdapter("X", "Y")).isEmpty();
        assertThat(reg.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("register: Doppel-Registrierung derselben (Klasse,Methode) führt zu IllegalStateException")
    void duplicate_registration_throws() {
        InMemorySqljRegistry reg = new InMemorySqljRegistry();
        SqljAdapter<Object> a1 = new TestAdapter("C", "M");
        SqljAdapter<Object> a2 = new TestAdapter("C", "M");
        reg.register(a1);
        assertThatThrownBy(() -> reg.register(a2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Doppelte Registrierung");
        assertThat(reg.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("thread-safety: Parallele Registrierung vieler unterschiedlicher Schlüssel funktioniert und ist konsistent")
    void thread_safety_register_many() throws InterruptedException {
        InMemorySqljRegistry reg = new InMemorySqljRegistry();
        int threads = 8;
        int countPerThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final int base = t * countPerThread;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < countPerThread; i++) {
                        String cls = "C" + (base + i);
                        String mtd = "M" + (base + i);
                        reg.register(new TestAdapter(cls, mtd));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        // Gleichzeitig starten
        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        // Erwartung: exakt threads*countPerThread Einträge
        assertThat(reg.size()).isEqualTo(threads * countPerThread);
        // Stichproben-Check
        assertThat(reg.findAdapter("C0", "M0")).isPresent();
        assertThat(reg.findAdapter("C17", "M17")).isPresent();
        assertThat(reg.findAdapter("C399", "M399")).isPresent();
    }

    @Test
    @DisplayName("thread-safety: Parallele Doppel-Registrierung derselben Kombination führt zu maximal 1 Eintrag")
    void thread_safety_duplicate_race() throws InterruptedException {
        InMemorySqljRegistry reg = new InMemorySqljRegistry();
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    try {
                        reg.register(new TestAdapter("SameC", "SameM"));
                    } catch (IllegalStateException ignored) {
                        // Erwartet: nur einer gewinnt, die übrigen werfen IllegalStateException
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(5, TimeUnit.SECONDS);
        pool.shutdownNow();

        // Genau 1 Eintrag erlaubt
        assertThat(reg.size()).isEqualTo(1);
        assertThat(reg.findAdapter("SameC", "SameM")).isPresent();
    }

    /**
     * Einfacher Test-Adapter mit konfigurierbaren Klassennamen/Methodennamen.
     */
    static class TestAdapter implements SqljAdapter<Object> {
        private final String cls;
        private final String mtd;

        TestAdapter(String cls, String mtd) {
            this.cls = cls;
            this.mtd = mtd;
        }

        @Override
        public String sqljClassName() {
            return cls;
        }

        @Override
        public String sqljMethodName() {
            return mtd;
        }

        @Override
        public Object execute(Connection connection, Map<String, Object> params) throws SQLException {
            return null;
        }
    }
}
