# syntax=docker/dockerfile:1.6
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
ENV MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true"
COPY pom.xml ./pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY trip-service/pom.xml trip-service/pom.xml
COPY ticket-service/pom.xml ticket-service/pom.xml
RUN mvn -B -q -pl trip-service -am dependency:go-offline
COPY trip-service/src trip-service/src
RUN mvn -B -q -pl trip-service -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/trip-service/target/*.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
