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
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-c", "standalone-full.xml", "-b", "0.0.0.0"]
