---
description: "Use when: configuring application.yml or adding new Spring Boot profiles — generates YAML configuration for datasource, JPA, logging, server settings, and custom properties."
agent: "agent"
tools: [read, edit, search]
argument-hint: "What to configure — e.g., 'Set up Oracle datasource config', 'Add Redis caching config', 'Create prod profile'"
---

# Application Configuration

Generate or update **Spring Boot application.yml** configuration files.

## Base Configuration — `application.yml`

```yaml
spring:
  application:
    name: ${APP_NAME}
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

  datasource:
    url: ${DB_URL:jdbc:oracle:thin:@localhost:1521/XEPDB1}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: oracle.jdbc.OracleDriver
    hikari:
      maximum-pool-size: ${DB_POOL_SIZE:10}
      minimum-idle: 5
      idle-timeout: 30000
      connection-timeout: 20000
      max-lifetime: 1800000
      pool-name: ${APP_NAME}-pool

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false   # Prevent lazy loading in controllers
    properties:
      hibernate:
        dialect: org.hibernate.dialect.OracleDialect
        default_schema: ${DB_SCHEMA}
        format_sql: true
        jdbc:
          batch_size: 25
        order_inserts: true
        order_updates: true

server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: /
  error:
    include-message: never
    include-stacktrace: never
    include-binding-errors: never

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method

logging:
  level:
    root: INFO
    com.company.app: INFO
    org.hibernate.SQL: WARN
    org.hibernate.type.descriptor.sql: WARN
```

## Dev Profile — `application-dev.yml`

```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    com.company.app: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

## Prod Profile — `application-prod.yml`

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10

  jpa:
    show-sql: false
    properties:
      hibernate:
        generate_statistics: false

server:
  error:
    include-message: never
    include-stacktrace: never

logging:
  level:
    root: WARN
    com.company.app: INFO
```

## Rules

- **Never** hardcode credentials — always use environment variables (`${DB_PASSWORD}`)
- Set `spring.jpa.open-in-view: false` — always
- Set `spring.jpa.hibernate.ddl-auto: validate` — schema is managed externally
- Use `server.error.include-stacktrace: never` in all profiles
- Dev profile enables SQL logging; prod profile disables it
- Use `spring.config.import` for external config if needed
