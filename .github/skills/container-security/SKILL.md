---
name: container-security
description: "Guide for securing Docker containers and Kubernetes deployments for Java Spring Boot applications, fixing container scan findings (Trivy, Grype, Snyk), writing secure Dockerfiles, and hardening runtime configurations."
---

# Container Security — Java Spring Boot

This skill guides the AI to build secure container images for Java Spring Boot applications, fix container scanning findings, and harden container runtime configurations.

## When to Use

- Writing or reviewing Dockerfiles for Spring Boot applications
- Fixing vulnerabilities reported by Trivy, Grype, or Snyk Container scans
- Hardening Kubernetes deployment manifests
- Reducing container image attack surface
- Responding to container-related security audit findings

---

## Secure Dockerfile for Spring Boot

### Recommended Multi-Stage Build

```dockerfile
# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup --system app && adduser --system --ingroup app app

WORKDIR /app
COPY --from=builder --chown=app:app /build/target/*.jar app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

### Distroless Alternative (Maximum Security)

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM gcr.io/distroless/java21-debian12
COPY --from=builder /build/target/*.jar /app.jar
USER nonroot:nonroot
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Base Image Selection

| Base Image | Size | Attack Surface | Shell | Use Case |
|-----------|------|---------------|-------|----------|
| `eclipse-temurin:21-jre` | ~200MB | Medium | Yes | Development |
| `eclipse-temurin:21-jre-alpine` | ~100MB | Small | Yes | Production (most teams) |
| `gcr.io/distroless/java21` | ~100MB | Minimal | No | Hardened production |
| `amazoncorretto:21-alpine` | ~100MB | Small | Yes | AWS deployments |

### Key Dockerfile Rules

1. **Pin image versions** — never use `:latest`:
   ```dockerfile
   FROM eclipse-temurin:21.0.3_9-jre-alpine
   ```

2. **Run as non-root**:
   ```dockerfile
   RUN addgroup --system app && adduser --system --ingroup app app
   USER app
   ```

3. **No secrets in images**:
   ```dockerfile
   # BAD
   ENV DB_PASSWORD=secret

   # GOOD — inject at runtime
   # docker run -e DB_PASSWORD=$DB_PASSWORD myapp
   ```

4. **Use .dockerignore**:
   ```dockerignore
   .git
   .gitignore
   .github/
   .idea/
   .vscode/
   target/
   *.md
   docker-compose*.yml
   Dockerfile
   .env
   .env.*
   work/
   ```

5. **Health checks**:
   ```dockerfile
   HEALTHCHECK --interval=30s --timeout=5s CMD wget -qO- http://localhost:8080/actuator/health || exit 1
   ```

6. **Read-only filesystem**:
   ```bash
   docker run --read-only --tmpfs /tmp:rw,noexec,nosuid myapp
   ```

---

## Fixing Container Scan Findings

### Running Scans

```bash
# Trivy
trivy image myapp:latest
trivy image --severity HIGH,CRITICAL --exit-code 1 myapp:latest

# Grype
grype myapp:latest

# Scan Dockerfile
trivy config Dockerfile
hadolint Dockerfile
```

### Common Findings and Fixes

| Finding | Cause | Fix |
|---------|-------|-----|
| OS package CVE | Base image outdated | Update base image version |
| Java dependency CVE | Vulnerable JAR in target | Update in `pom.xml` |
| Running as root | No `USER` instruction | Add non-root user |
| `.git` in image | Missing `.dockerignore` | Add to `.dockerignore` |
| Build tools in runtime | No multi-stage build | Separate build and runtime stages |
| No health check | Missing `HEALTHCHECK` | Add `HEALTHCHECK` with actuator |
| Writable filesystem | No read-only enforcement | Use `--read-only` flag |

---

## Kubernetes Security Hardening

### Pod Security Context for Spring Boot

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: api-service
  template:
    metadata:
      labels:
        app: api-service
    spec:
      automountServiceAccountToken: false
      securityContext:
        runAsNonRoot: true
        runAsUser: 1001
        runAsGroup: 1001
        fsGroup: 1001
        seccompProfile:
          type: RuntimeDefault
      containers:
        - name: api-service
          image: api-service:1.0.0@sha256:abcdef...
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop:
                - ALL
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-credentials
                  key: password
          resources:
            limits:
              cpu: "1000m"
              memory: "1Gi"
            requests:
              cpu: "500m"
              memory: "512Mi"
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
            initialDelaySeconds: 15
            periodSeconds: 5
          volumeMounts:
            - name: tmp
              mountPath: /tmp
      volumes:
        - name: tmp
          emptyDir: {}
```

### Network Policy

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: api-service-policy
spec:
  podSelector:
    matchLabels:
      app: api-service
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: api-gateway
      ports:
        - port: 8080
  egress:
    - to:
        - podSelector:
            matchLabels:
              app: oracle-db
      ports:
        - port: 1521
    - to:  # Allow DNS
        - namespaceSelector: {}
      ports:
        - port: 53
          protocol: UDP
```
