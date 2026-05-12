---
description: "Use when: generating Docker and docker-compose files for a Spring Boot application — creates multi-stage Dockerfile, docker-compose with Oracle DB, and .dockerignore."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Describe what to containerize — e.g., 'Create Dockerfile for Spring Boot app with Oracle DB'"
---

# Docker Setup

Generate **Dockerfile** and **docker-compose.yml** for a Spring Boot application with Oracle Database.

## Dockerfile (multi-stage)

```dockerfile
# --- Build stage ---
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Security: run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
```

## docker-compose.yml

```yaml
version: '3.8'

services:
  app:
    build: .
    container_name: spring-app
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      DB_URL: jdbc:oracle:thin:@oracle-db:1521/XEPDB1
      DB_USERNAME: app_user
      DB_PASSWORD: ${DB_PASSWORD}
      DB_SCHEMA: APP_SCHEMA
    depends_on:
      oracle-db:
        condition: service_healthy
    networks:
      - app-network
    restart: unless-stopped

  oracle-db:
    image: gvenzl/oracle-xe:21-slim
    container_name: oracle-db
    ports:
      - "1521:1521"
    environment:
      ORACLE_PASSWORD: ${ORACLE_SYS_PASSWORD}
      APP_USER: app_user
      APP_USER_PASSWORD: ${DB_PASSWORD}
    volumes:
      - oracle-data:/opt/oracle/oradata
      - ./scripts/init-db:/container-entrypoint-initdb.d
    healthcheck:
      test: ["CMD", "healthcheck.sh"]
      interval: 30s
      timeout: 10s
      retries: 10
      start_period: 120s
    networks:
      - app-network

volumes:
  oracle-data:

networks:
  app-network:
    driver: bridge
```

## .dockerignore

```
target/
.git/
.github/
.idea/
*.iml
*.log
work/
docs/
README.md
docker-compose*.yml
.env
```

## .env (template — do NOT commit)

```env
DB_PASSWORD=changeme
ORACLE_SYS_PASSWORD=changeme
```

## Rules

- **Multi-stage build** — build with Maven, run with JRE only
- **Non-root user** — always run the app as a non-root user
- **No secrets in Dockerfile** — use environment variables or Docker secrets
- **Health check** — use Spring Boot Actuator `/actuator/health`
- **Container support** — use `-XX:+UseContainerSupport` and `-XX:MaxRAMPercentage`
- Add `.env` to `.gitignore` — never commit credentials
