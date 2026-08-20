FROM eclipse-temurin:17-jre
WORKDIR /app
ARG JAR_FILE=target/aaax-server-*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
