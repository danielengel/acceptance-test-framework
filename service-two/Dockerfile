FROM maven:3.9.14-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -B -ntp package -DskipTests
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/target/service-two-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
