FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY . .

RUN mvn -f minidev-backend/pom.xml -DskipTests clean package

FROM eclipse-temurin:25-jre
WORKDIR /app

ENV JAVA_OPTS=""


COPY --from=build /workspace/minidev-backend/target/*.war /app/app.war

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.war"]

