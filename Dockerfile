FROM eclipse-temurin:25-jre

WORKDIR /app
COPY target/part-service-0.0.1-SNAPSHOT.jar /app/part-service.jar

EXPOSE 32010
ENTRYPOINT ["java", "-jar", "/app/part-service.jar"]
