# Compila o WAR e executa os testes sem exigir Java/Maven no computador do usuário.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp verify

FROM quay.io/wildfly/wildfly:35.0.1.Final-jdk17
COPY --chown=jboss:root infra/configure.cli /tmp/configure.cli
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/tmp/configure.cli
COPY --from=build --chown=jboss:root /build/target/j2ee-patterns.war /opt/jboss/wildfly/standalone/deployments/
EXPOSE 8080
# O asset só responde depois do deploy e não cria uma sessão HTTP de usuário.
HEALTHCHECK --interval=5s --timeout=5s --start-period=30s --retries=24 \
  CMD curl --fail --silent --show-error --max-time 3 --output /dev/null http://127.0.0.1:8080/j2ee-patterns/assets/styles.css
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-c", "standalone-full.xml", "-b", "0.0.0.0"]
