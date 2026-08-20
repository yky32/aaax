FROM eclipse-temurin:21-jre
WORKDIR /app
ARG JAR_FILE=target/aaax-server-*.jar
COPY ${JAR_FILE} app.jar
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8081
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
