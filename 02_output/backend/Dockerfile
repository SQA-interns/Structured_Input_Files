# Backend image (eclipse-temurin JRE from tech-stack.md). Build the jar first: ./mvnw -B package -DskipTests
FROM eclipse-temurin:21.0.10_7-jre-alpine
RUN addgroup -S app && adduser -S -G app app \
    && mkdir -p /data/registrations \
    && chown app:app /data/registrations
COPY target/registration-backend-1.0.0.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
