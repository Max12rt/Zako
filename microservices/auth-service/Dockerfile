# syntax=docker/dockerfile:1.6
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
ENV MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true"
COPY pom.xml ../pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
RUN mvn -B -q -pl auth-service dependency:go-offline
COPY auth-service/src auth-service/src
RUN mvn -B -q -pl auth-service -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/auth-service/target/*.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
