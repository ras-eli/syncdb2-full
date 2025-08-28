FROM ibmcom/db2:11.5.8.0

# Deutsch: OpenJDK 11 hinzufügen (für den SQLJ-Translator notwendig)
USER root
RUN microdnf install -y java-11-openjdk-headless && microdnf clean all
ENV PATH="/usr/lib/jvm/java-11/bin:${PATH}"

# Deutsch: Wieder zurück zu db2inst1 falls nötig
USER db2inst1
