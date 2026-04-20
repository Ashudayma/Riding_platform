FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY ride-platform-app/pom.xml ./ride-platform-app/pom.xml
COPY ride-platform-app/src ./ride-platform-app/src

RUN mvn -q -pl ride-platform-app -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ENV JAVA_OPTS="" \
    SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=docker \
    LOG_FILE_PATH=/var/log/riding-platform/ride-platform-app.log

RUN mkdir -p /var/log/riding-platform

COPY --from=builder /workspace/ride-platform-app/target/ride-platform-app-0.1.0-SNAPSHOT.jar /app/ride-platform-app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/ride-platform-app.jar"]
