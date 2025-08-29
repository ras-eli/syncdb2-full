# Deutsch: Stage 1 – JRE 11 aus schlankem Temurin-Image
FROM eclipse-temurin:11-jre AS jre

# Deutsch: Stage 2 – Db2-Basis mit SQLJ-Translator
FROM ibmcom/db2:11.5.8.0

# Deutsch: JRE in das Db2-Image kopieren (kein Paketmanager nötig)
COPY --from=jre /opt/java/openjdk /opt/jre
ENV PATH="/opt/jre/bin:${PATH}"

# Deutsch: Als root ausführen (db2inst1 wird ohne EntryPoint nicht angelegt)
USER root
