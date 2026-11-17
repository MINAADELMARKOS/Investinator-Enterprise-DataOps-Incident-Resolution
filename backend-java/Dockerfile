# Build the verified jar with ./mvnw verify before building this runtime image.
FROM eclipse-temurin:21-jre-jammy@sha256:dff6eb46527bd293a3c06bbbe9e64e8c9f91f01905b5abbdad31761c03059d2c
WORKDIR /app
COPY --chown=10001:10001 target/backend-java-0.1.0-SNAPSHOT.jar /app/app.jar
USER 10001:10001
ENV JAVA_BIND_ADDRESS=0.0.0.0 JAVA_PORT=8081
EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=6 CMD curl --fail --silent http://127.0.0.1:8081/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
