# Backend image (architecture.md: eclipse-temurin base, non-root, SB-11).
# Build the jar first: ./mvnw -B -DskipTests package
FROM eclipse-temurin:21.0.10_7-jre-alpine
RUN addgroup -S app && adduser -S -G app -u 10001 app \
    && mkdir -p /data/json-copies \
    && chown app:app /data/json-copies
WORKDIR /app
COPY target/registration-backend-0.1.0.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
