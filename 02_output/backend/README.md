# Backend

> Written in: phases 0 and 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Spring Boot REST API for the conference registration (root package `si.konferenca.registration`).

## Commands (ES-05)

Run from `02_output/backend` (Git Bash or any POSIX shell; `mvnw.cmd` on Windows cmd).

| Purpose | Command |
|---|---|
| build | `./mvnw -B package -DskipTests` |
| test | `./mvnw -B verify` |
| check (format, lint, duplication, static analysis) | `./mvnw -B -Pcheck -DskipTests verify` |
| run | `./mvnw spring-boot:run` (needs PostgreSQL and SMTP settings; see the root README for `docker compose`) |
| dependency scan | `NVD_API_KEY=… ./mvnw -B -Pdepcheck org.owasp:dependency-check-maven:12.1.0:check` |
| mutation | `./mvnw -B test-compile org.pitest:pitest-maven:mutationCoverage` |
