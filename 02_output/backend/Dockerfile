# Runtime image only; build the jar first with ./mvnw -B package -DskipTests (README).
FROM eclipse-temurin:21.0.10_7-jre-alpine

# Non-root user (SB-11); JSON copies live on a volume mounted at /data/json-copies.
RUN addgroup -S app && adduser -S -G app app \
    && mkdir -p /data/json-copies && chown -R app:app /data
WORKDIR /app
COPY target/registration-backend-1.0.0.jar /app/app.jar
USER app

ENV APP_JSON_COPY_DIR=/data/json-copies
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
