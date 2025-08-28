# spring-support — راهنمای فارسی

این ماژول امکانات ادغام **Native** با تراکنش‌های Spring/Spring Boot را برای اجرای **SQLJ** و ثبت/ارسال **Outbox قبل از
Commit** فراهم می‌کند؛ بدون نیاز به ارکستریتور مرکزی.

---

## چه چیزی فراهم می‌شود؟

- **اجرای SQLJ روی همان Connection تراکنشی Spring**: `SqljExecutorSpring.execute(...)`
- **Outbox قبل از Commit در همان تراکنش**: `SpringTxOutbox.record(...)` (اختیاری)
- **پشتیبانی از Spring Boot Auto-Configuration**: رجیستری آداپترها، پورت Outbox، و پیش‌فرض‌های Outbox به‌صورت خودکار
  ساخته می‌شوند.

## وابستگی Maven

```xml

<dependency>
    <groupId>osplus</groupId>
    <artifactId>spring-support</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## استفاده با Spring Boot (Auto-Config)

1) فقط وابستگی را اضافه کنید. Boot به‌صورت خودکار:
    - `SqljRegistry` (InMemory) می‌سازد و همه‌ی Beanهای `SqljAdapter` را ثبت می‌کند.
    - `MqSendPort` پیش‌فرض می‌سازد (`SendToMqPortImpl`).
    - `SpringTxOutbox.setDefaults(dataSource, mqSendPort)` را فراخوانی می‌کند.

2) در `application.yml` (نمونه):

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

3) در سرویس‌ها:

```java

@Transactional
public void flow() throws SQLException {
    SqljExecutorSpring.execute(sqljRegistry, dataSource,
            "demo.sqlj.OrderSqlj", "insertOrder", Map.of("note", "A"));

    // Outbox اختیاری
    SpringTxOutbox.record(EnvelopeFactory.create("demo.sqlj.OrderSqlj", "insertOrder", Map.of("note", "A"), "v1", "corr", null, null));
}
```

## استفاده با Spring (بدون Boot)

1) Beanها را خودتان تعریف کنید: `SqljRegistry`، `SendToMqRepository`، `MqSendPort`، و در صورت نیاز آداپترها.
2) یک‌بار پیش‌فرض Outbox را ست کنید:

```java
SpringTxOutbox.setDefaults(primaryDataSource, mqSendPort);
```

3) سپس مانند Boot استفاده کنید.

## اجرای SQLJ ۱۰۰٪ تراکنشی

- `SqljExecutorSpring` از `DataSourceUtils.getConnection(ds)` استفاده می‌کند تا **همان Connection تراکنشی bind‌شده** به
  وسیله‌ی TransactionManager فعلی را بگیرد. در نتیجه Commit/Rollback با Spring هماهنگ است.
- هر Exception از SQLJ به Spring پاس داده می‌شود تا Rollback تصمیم‌گیری شود.

## Outbox قبل از Commit

- `SpringTxOutbox.record(...)` پاکت‌ها را در بافر وابسته به تراکنش جمع می‌کند.
- `beforeCommit()` با همان Connection تراکنشی، همه را ارسال می‌کند. خطا ⇒ Rollback کل تراکنش.

## چند دیتابیس/چند TM

- می‌توانید از امضاهای منعطف استفاده کنید:
    - `record(envelope)` ← استفاده از پیش‌فرض‌ها
    - `record(envelope, DataSource)` ← تعیین DS مشخص با پورت پیش‌فرض
    - `recordWithTxManager(envelope, DataSource, MqSendPort)` ← کنترل کامل

## نکات تست/اشکال‌زدایی

- برای تست ساده از H2 و `DataSourceTransactionManager` استفاده کنید (نمونه‌ها در ماژول وجود دارد).
- اگر خطای `Keine aktive Spring-Transaktion` گرفتید، مطمئن شوید متدتان `@Transactional` دارد.
- برای `Instant` در JSON، ماژول `jackson-datatype-jsr310` اضافه شده است.
