# Runtime image of the backend. Build the jar first: ./mvnw -B package -DskipTests
FROM eclipse-temurin:21.0.10_7-jre-alpine
RUN addgroup -S -g 10001 app \
 && adduser -S -D -H -u 10001 -G app app \
 && mkdir -p /app /data/json-copies \
 && chown -R app:app /data
WORKDIR /app
COPY target/registration-backend-0.1.0.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
