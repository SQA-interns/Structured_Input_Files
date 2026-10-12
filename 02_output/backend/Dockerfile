# Runtime image only: the jar is built on the host with ./mvnw package (tech-stack.md lists the JRE image).
FROM eclipse-temurin:21.0.10_7-jre-alpine
RUN addgroup -S app && adduser -S -G app -H -h /app app \
 && mkdir -p /app /data/json-copies \
 && chown app:app /data/json-copies
WORKDIR /app
COPY target/registration-backend-1.0.0.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
