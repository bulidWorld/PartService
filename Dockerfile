FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build
COPY pom.xml .
COPY maven_settings.xml ./maven_settings.xml
COPY src ./src
RUN mvn -s ./maven_settings.xml package -Dmaven.test.skip=true

FROM eclipse-temurin:25-jre

WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-venv \
    && rm -rf /var/lib/apt/lists/*
COPY scripts/requirements.txt /app/scripts/requirements.txt
RUN python3 -m venv /app/.venv \
    && /app/.venv/bin/pip install --no-cache-dir -r /app/scripts/requirements.txt
COPY scripts/sync_schema.py /app/scripts/sync_schema.py
COPY src/main/resources/db /app/src/main/resources/db
COPY --from=build /build/target/part-service-0.0.1-SNAPSHOT.jar /app/part-service.jar

EXPOSE 32010
ENTRYPOINT ["/bin/sh", "-c", "/app/.venv/bin/python -u /app/scripts/sync_schema.py && exec java -jar /app/part-service.jar"]
