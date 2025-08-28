# spring-support — Deutsche Anleitung

Dieses Modul bietet eine **native Integration** in Spring/Spring Boot-Transaktionen für die Ausführung von **SQLJ**
sowie das **Outbox-Senden vor dem Commit**, ohne zentralen Orchestrator.

---

## Funktionsumfang

- **Ausführung von SQLJ auf derselben, transaktionalen Spring-Connection**: `SqljExecutorSpring.execute(...)`
- **Outbox vor Commit innerhalb derselben TX**: `SpringTxOutbox.record(...)` (optional)
- **Spring Boot Auto-Configuration**: Registriert automatisch alle `SqljAdapter`-Beans, erzeugt `MqSendPort` und setzt
  `SpringTxOutbox`-Defaults.

## Maven-Abhängigkeit

```xml

<dependency>
    <groupId>osplus</groupId>
    <artifactId>spring-support</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Verwendung mit Spring Boot

1) Dependency hinzufügen – Auto-Config übernimmt:
    - `SqljRegistry` (InMemory) + Registrierung aller `SqljAdapter` aus dem Kontext
    - `MqSendPort` (Default: `SendToMqPortImpl`)
    - `SpringTxOutbox.setDefaults(dataSource, mqSendPort)`

2) Beispiel `application.yml`:

```yaml
syncdb2:
  mq:
    send:
      procedure-name: SEND_TO_MQ
    read:
      procedure-name: READ_FROM_MQ
      batch-size: 50
      fixed-delay: 2000
  logging:
    masked-params: [ "password", "iban", "cardNumber" ]
```

3) Service-Beispiel:

```java

@Transactional
public void flow() throws SQLException {
    SqljExecutorSpring.execute(sqljRegistry, dataSource,
            "demo.sqlj.OrderSqlj", "insertOrder", Map.of("note", "A"));

    // Optionale Outbox
    SpringTxOutbox.record(EnvelopeFactory.create("demo.sqlj.OrderSqlj", "insertOrder", Map.of("note", "A"), "v1", "corr", null, null));
}
```

## 100% Transaktionalität

- `SqljExecutorSpring` nutzt `DataSourceUtils.getConnection(ds)`, um die **von Spring gebundene Connection** zu
  erhalten; Commit/Rollback steuert der TransactionManager.
- Exceptions werden nicht geschluckt → Spring kann korrekt rollbacken.

## Outbox vor Commit

- `SpringTxOutbox.record(...)` sammelt Envelopes pro Transaktion/DataSource.
- In `beforeCommit()` erfolgt der Versand auf derselben Connection. Fehler ⇒ Rollback der gesamten Transaktion.

## Multi-DataSource / Multi-TM

- Flexible API-Überladungen:
    - `record(envelope)` (Defaults),
    - `record(envelope, DataSource)`,
    - `recordWithTxManager(envelope, DataSource, MqSendPort)`.

## Troubleshooting

- Fehlermeldung „Keine aktive Spring-Transaktion“ ⇒ `@Transactional` fehlt.
- Für `Instant`-Serialization ist `jackson-datatype-jsr310` im Core bereits enthalten.
- H2 + `DataSourceTransactionManager` können für Integrationstests verwendet werden (siehe vorhandene ITs).
