# Runtime image only; build the jar first with ./mvnw -B package (see README).
FROM eclipse-temurin:21.0.10_7-jre-alpine
RUN addgroup -S -g 1000 app && adduser -S -u 1000 -G app app \
    && mkdir -p /data/registrations && chown app:app /data/registrations && chmod 700 /data/registrations
WORKDIR /app
COPY --chown=app:app target/registration-backend-0.1.0.jar /app/app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
