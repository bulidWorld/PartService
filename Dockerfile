FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /build/target/part-service-0.0.1-SNAPSHOT.jar /app/part-service.jar

EXPOSE 32010
ENTRYPOINT ["java", "-jar", "/app/part-service.jar"]
