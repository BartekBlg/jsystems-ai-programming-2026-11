# syntax=docker/dockerfile:1
# Build context = repository root (ADR-001 §3.8). Build from the repo root:
#   docker build -f app/Dockerfile -t hsdc-poc:local .
# No secrets are baked into the image: supply OPENROUTER_API_KEY at run time.

# ---- build: Maven builds the backend and, via frontend-maven-plugin, the Angular SPA
FROM eclipse-temurin:25.0.1_8-jdk AS build
WORKDIR /src

# Same relative layout as the repository: pom.xml resolves policies via ../../docs/policies
COPY app/ /src/app/
COPY docs/policies/ /src/docs/policies/

WORKDIR /src/app/backend
RUN chmod +x ./mvnw \
 && ./mvnw -B package -DskipTests

# ---- runtime: JRE only, non-root
FROM eclipse-temurin:25.0.1_8-jre AS runtime

RUN groupadd --system --gid 10001 app \
 && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app

WORKDIR /app
COPY --from=build --chown=app:app /src/app/backend/target/service-copilot-0.0.1-SNAPSHOT.jar /app/app.jar

USER app
EXPOSE 8080

# The JRE image has no guaranteed curl/wget, so the check uses bash's /dev/tcp (bash is in the base image).
# It sends a plain HTTP/1.0 GET and passes only on a 200 status line (Actuator returns 503 when DOWN).
HEALTHCHECK --interval=10s --timeout=5s --start-period=20s --retries=3 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/${SERVER_PORT:-8080} && printf 'GET /actuator/health HTTP/1.0\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n' >&3 && head -n1 <&3 | grep -q ' 200 '"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
