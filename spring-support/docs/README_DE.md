# 📘 syncdb2 :: spring-support — Benutzerhandbuch (Deutsch)

Dieses Modul integriert **Transactional Outbox** nahtlos in **Spring-Transaktionen**, ohne einen zentralen Orchestrator
zu benötigen. Services rufen nach jedem erfolgreichen SQLJ-Aufruf lediglich `SpringTxOutbox.record(...)` auf; die Outbox
wird **automatisch vor Commit** mit **derselben transaktionalen Connection** versendet.

---

## 1) Motivation & Zielbild

- **Atomicity:** Fachliche Änderungen (Primary-DB) und Outbox-Schreibvorgang sind in **einer** Transaktion; Fehler beim
  Senden ⇒ **Rollback**.
- **Imperativer Stil:** Kein Orchestrator. Services (auch verteilt) können frei SQLJ aufrufen und anschließend
  `record(...)` nutzen.
- **Mehrere DataSources:** Pro DataSource innerhalb der Transaktion eigener Puffer & Synchronization.
- **Testbarkeit:** Sendeweg über Port (`MqSendPort`) abstrahiert.

---

## 2) Architekturüberblick

```
Service (@Transactional)      SpringTxOutbox                  DB (Primary)
-------------------------     -----------------------------   ---------------------
SQLJ (erfolgreich) ----->     record(envelope, ds, port)
                              [Tx-gebundener Buffer je DS]
... weitere Arbeit ...

Commit ------------------>     TransactionSynchronization.beforeCommit()
                              DataSourceUtils.getConnection(ds)  ---> selbe Connection
                              port.sendWithinTx(conn, envelope*) ---> SP: SEND_TO_MQ
                              (Exception? -> RuntimeException -> Rollback)

                              afterCompletion() -> Buffer-Cleanup
```

**Wichtig:** `DataSourceUtils.getConnection(ds)` liefert die **transaktionsgebundene** Connection, d. h. Commit/Rollback
wird ausschließlich vom Spring-TransactionManager gesteuert.

---

## 3) Hauptkomponenten

### 3.1 `SpringTxOutbox`

- Thread-lokaler Puffer: `Map<DataSource, List<SyncEnvelope>>`
- Registrierung einer `TransactionSynchronization` je DataSource
- `beforeCommit(readOnly)`:
    - readOnly + Buffer≠leer ⇒ `IllegalStateException` (Schreibzugriff in readOnly unzulässig)
    - ansonsten: Connection via `DataSourceUtils.getConnection(ds)` → `MqSendPort.sendWithinTx(...)` für alle Envelopes
- `afterCompletion(status)`:
    - Puffer- und Registrierungsdaten säubern

### 3.2 `MqSendPort`

- Port für „senden **innerhalb** der Transaktion“.
- Erhält die bereits durch Spring gebundene Connection.

### 3.3 `SendToMqPortImpl`

- Standard-Port: Serialisiert `SyncEnvelope` kanonisch → ruft `SendToMqRepository.callSendToMq(...)` auf.
- `status != 0` ⇒ `MqSendException` ⇒ Rollback.

---

## 4) Designentscheidungen (mit Begründung)

- **beforeCommit statt afterCommit:** Outbox ist Teil derselben Transaktion ⇒ nur in `beforeCommit` ist die
  DB-Transaktion noch offen.
- **Key = DataSource:** Pro DataSource eine Synchronization; klare Trennung bei Multi-DS-Transaktionen.
- **Keine „tm.getConnection()“ API:** In Spring ist der korrekte Weg `DataSourceUtils.getConnection(ds)`; dies liefert
  die gebundene transaktionale Connection.
- **readOnly-Transaktionen:** Schützen vor versehentlichen Outbox-Schreibzugriffen in readOnly-Kontext.
- **Port-Abstraktion:** Erhöht Testbarkeit & Austauschbarkeit (z. B. alternative SP, Logging etc.).

---

## 5) Einbindung (Beispiel ohne Boot)

```java
DataSource ds = ...;
DataSourceTransactionManager txm = new DataSourceTransactionManager(ds);

TransactionStatus s = txm.getTransaction(new DefaultTransactionDefinition());

// Business
// SQLJ-Aufruf erfolgreich -> Envelope erstellen
SyncEnvelope e = EnvelopeFactory.create("C", "M", Map.of("k", 1), "v1", "corr", null, null);

// Port wählen
MqSendPort port = new SendToMqPortImpl(new SendToMqRepository());

// registrieren
SpringTxOutbox.

record(e, ds, port);

// Commit -> Outbox wird in beforeCommit via selbe Connection geschrieben
txm.

commit(s);
```

---

## 6) Integration in Spring Boot (Kurz)

- Beans definieren:
    - `DataSource` (primary)
    - `SendToMqRepository`
    - `MqSendPort = new SendToMqPortImpl(repository)`
- In Services mit `@Transactional("primaryTxManager")` nach jedem SQLJ:
  ```java
  SpringTxOutbox.record(envelope, primaryDataSource, mqSendPort);
  ```
- Optional: weitere DataSources analog.

---

## 7) Tests

- **Unit (`SendToMqPortImplTest`)**: Erfolg und Fehlerstatus (`MqSendException`).
- **Integration (H2 + `DataSourceTransactionManager`)**:
    - **Erfolg:** Mehrere `record(...)` in einer TX → vor Commit Outbox geschrieben; Business-Insert mit-committet.
    - **Fehler:** Port wirft Exception → Commit wirft `RuntimeException` → **Rollback** von Outbox und Business.

---

## 8) Grenzen & Hinweise

- **JTA/XA:** Das Muster funktioniert mit DataSource-gebundenen Ressourcen. Für komplexe XA-Szenarien muss die
  DataSource entsprechend konfiguriert sein.
- **readOnly:** Verwendung in readOnly-TXs ist untersagt (Fehler bei Commit).
- **Fehlerbehandlung:** Exceptions im `beforeCommit` müssen „hoch“ propagieren (kein Catch/Swallow), damit Spring rollt.

---

## 9) Erweiterungen (Future Work)

- Batch-Senden (statt einzeln) inklusive SP-Unterstützung
- Metriken/Tracing (Micrometer) um Anzahl/Größe Outbox-Batches
- Retry-Strategien für sporadische DB-Fehler (konfigurierbar)
- Declarative Interceptor um `record(...)` bei Erfolg automatisch zu „injektieren“

---

Viel Erfolg bei der Integration! ✨
