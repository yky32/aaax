# Local clone image — not a production hardening guide.
# Build: docker compose up --build
# Runtime still needs Postgres + Redis (same compose file).

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY .mvn .mvn
COPY src src
RUN mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/aaax-0.9.1.jar /app/aaax.jar
EXPOSE 8081
ENV SERVER_PORT=8081
ENTRYPOINT ["java", "-jar", "/app/aaax.jar"]
