FROM maven:3-eclipse-temurin-26 AS build

WORKDIR /app

COPY pom.xml .
COPY src src

RUN mvn --no-transfer-progress -DskipTests package


FROM eclipse-temurin:25-jre

WORKDIR /app

RUN groupadd --system filmoteka \
    && useradd --system --gid filmoteka --no-create-home filmoteka \
    && apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build --chown=filmoteka:filmoteka /app/target/*.jar /app/app.jar

USER filmoteka:filmoteka

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
