# Deutsch: JRE aus einem schlanken Temurin-Image holen (Stage 1)
FROM eclipse-temurin:11-jre AS jre

# Deutsch: Basis mit SQLJ-Translator (Stage 2)
FROM ibmcom/db2:11.5.8.0

# Deutsch: JRE in das Db2-Image kopieren, ohne Paketmanager
COPY --from=jre /opt/java/openjdk /opt/jre
ENV PATH="/opt/jre/bin:${PATH}"

# Deutsch: Als Standardbenutzer weiterarbeiten (sqlj liegt unter /opt/ibm/db2/...)
USER db2inst1
