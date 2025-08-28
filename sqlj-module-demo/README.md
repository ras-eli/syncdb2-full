# SQLJ Modul Demo (Insert/Update/Delete für zwei Tabellen)

**Hinweis (Deutsch):**
Dieses Modul zeigt eine minimal lauffähige SQLJ-Struktur in einem Maven-Projekt.
- Zwei Tabellen: PERSON(ID, NAME), PRODUCT(ID, TITLE, PRICE)
- Für jede Tabelle: je eine Klasse pro Operation (Insert, Update, Delete) als SQLJ.
- SQLJ-Übersetzung via `exec-maven-plugin` (offline per Default, online via Profil `sqlj-online`).
- Kommentare innerhalb von Code sind **auf Deutsch**, außenliegende Anleitung hier ebenfalls.

## Voraussetzungen
- IBM DB2 Client/SDK installiert, `sqlj`-Befehl im `PATH`.
- JARs `db2jcc4.jar` und `sqljrt.jar` ggf. lokal in Maven installieren:
```
mvn install:install-file -Dfile=/opt/ibm/db2/java/db2jcc4.jar -DgroupId=com.ibm.db2 -DartifactId=db2jcc4 -Dversion=11.5.0.0 -Dpackaging=jar
mvn install:install-file -Dfile=/opt/ibm/db2/java/sqljrt.jar  -DgroupId=com.ibm.db2 -DartifactId=sqljrt  -Dversion=11.5.0.0 -Dpackaging=jar
```

## Tabellen (Beispiel)
```sql
CREATE TABLE PERSON (
  ID   INT PRIMARY KEY,
  NAME VARCHAR(100) NOT NULL
);

CREATE TABLE PRODUCT (
  ID    INT PRIMARY KEY,
  TITLE VARCHAR(100) NOT NULL,
  PRICE DECIMAL(10,2) NOT NULL
);
```

## Build
- Offline (ohne DB-Checker): `mvn clean package`
- Online (mit Checker): `mvn clean package -Psqlj-online -Ddb.url=jdbc:db2://localhost:50000/TESTDB -Ddb.user=db2inst1 -Ddb.pass=secret`

