# ─── Stage 1: Frontend ───────────────────────────────────────────────
FROM node:26-alpine AS frontend
WORKDIR /app/minidev-frontend

COPY minidev-frontend/package.json minidev-frontend/package-lock.json* ./
RUN --mount=type=cache,target=/root/.npm npm ci --prefer-offline

COPY minidev-frontend/ ./
RUN npm run build
# Output lands at /app/minidev-backend/src/main/resources/static/ via angular.json outputPath


# ─── Stage 2: Backend ────────────────────────────────────────────────
FROM maven:3.9.15-eclipse-temurin-26 AS build
WORKDIR /workspace

# Copy pom.xml only — dependency layer stays cached until pom changes
COPY minidev-backend/pom.xml minidev-backend/

# Copy pre-built frontend into backend resources (where Spring Boot expects it)
COPY --from=frontend /app/minidev-backend/src/main/resources/static/ \
     minidev-backend/src/main/resources/static/

# Copy source code
COPY minidev-backend/src minidev-backend/src

# Build with Maven — .m2 cache survives across builds via BuildKit
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -f minidev-backend/pom.xml \
    -DskipTests -DskipFrontendBuild=true \
    package


# ─── Stage 3: Runtime ────────────────────────────────────────────────
FROM eclipse-temurin:25-jre
WORKDIR /app

ENV JAVA_OPTS=""

COPY --from=build /workspace/minidev-backend/target/*.war /app/app.war

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.war"]
