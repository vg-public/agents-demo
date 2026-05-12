---
description: "Use when: adding Spring Boot Actuator health checks, custom health indicators, or configuring readiness/liveness probes for Kubernetes deployments."
agent: "agent"
tools: [read, edit, search]
argument-hint: "What to monitor — e.g., 'Add health check for Oracle DB and external API' or 'Configure K8s probes'"
---

# Add Health Checks

Add **Spring Boot Actuator health checks** and custom health indicators for monitoring and container orchestration.

## Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## Configuration — `application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized  # never | when_authorized | always
      probes:
        enabled: true  # enables /actuator/health/liveness and /readiness
  health:
    db:
      enabled: true
    diskspace:
      enabled: true
```

## Custom Health Indicator — `config/health/<Name>HealthIndicator.java`

```java
@Component
public class OracleHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public OracleHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            String result = jdbcTemplate.queryForObject(
                "SELECT 'OK' FROM DUAL", String.class);
            return Health.up()
                .withDetail("database", "Oracle")
                .withDetail("status", result)
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "Oracle")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## External Service Health Check

```java
@Component
public class ExternalApiHealthIndicator implements HealthIndicator {

    private final RestClient restClient;
    private final String healthUrl;

    public ExternalApiHealthIndicator(
            RestClient.Builder builder,
            @Value("${app.external-api.health-url}") String healthUrl) {
        this.restClient = builder.build();
        this.healthUrl = healthUrl;
    }

    @Override
    public Health health() {
        try {
            ResponseEntity<Void> response = restClient.get()
                .uri(healthUrl)
                .retrieve()
                .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up().withDetail("url", healthUrl).build();
            }
            return Health.down()
                .withDetail("url", healthUrl)
                .withDetail("status", response.getStatusCode().value())
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("url", healthUrl)
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## Kubernetes Probes

```yaml
# docker-compose or K8s deployment
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 20
  periodSeconds: 5

startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  failureThreshold: 30
  periodSeconds: 10
```

## Custom Readiness Group

```yaml
management:
  endpoint:
    health:
      group:
        readiness:
          include: db, diskSpace, externalApi
        liveness:
          include: ping
```

## Rules

- Never expose `show-details: always` in production — use `when_authorized`
- Don't expose sensitive endpoints (env, configprops, beans) without authentication
- Health checks should be **fast** (< 2 seconds) — don't do expensive operations
- Use `Health.down()` only for critical dependencies that prevent serving requests
- Separate liveness (app alive?) from readiness (app ready for traffic?)
- Custom health indicators auto-register — name derived from class prefix
