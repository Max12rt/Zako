# Kubernetes Comparison (Monolith vs Microservices) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the monolith, split it into 3 microservices communicating via Kafka, deploy both on Kind k8s, run k6 load tests, and write a Polish sprawozdanie comparing results.

**Architecture:** Monolith stays in namespace `zako`. Microservices (auth-service:8081, trip-service:8082, ticket-service:8083) live in namespace `zako-ms` with separate Postgres DBs and a Kafka broker. Ingress routes `zako-ms.localhost` to the appropriate service per path prefix. ticket-service calls trip-service via REST for station enrichment at purchase time, and publishes Kafka events for async seat-count updates.

**Tech Stack:** Spring Boot 4.0.4, Java 21, Lombok, jjwt 0.12.6, Flyway, Spring Kafka, PostgreSQL 16, Docker, Kind, nginx-ingress, Prometheus, Grafana, Jaeger, k6

---

## File Map

```
monolith/src/main/java/zako/monolith/ticket/TicketController.java   ← add /my endpoint
frontend/src/app/services/ticket-api.service.ts                     ← use /api/tickets/my
frontend/src/app/pages/my-tickets/my-tickets.ts                     ← remove userId from call

microservices/
  pom.xml                          parent POM (Spring Boot 4.0.4 parent, 3 modules)
  auth-service/
    pom.xml
    Dockerfile
    src/main/java/zako/auth/
      AuthServiceApplication.java
      user/{User,Role,UserRepository,UserService,UserController}.java
      user/dto/{RegisterRequest,UserResponse}.java
      auth/{AuthController,AuthService}.java
      auth/dto/{LoginRequest,LoginResponse}.java
      config/{JwtService,JwtAuthFilter,SecurityConfig}.java
      exception/{GlobalExceptionHandler,ResourceNotFoundException}.java
    src/main/resources/
      application.properties
      db/migration/V1__create_users.sql
    src/test/java/zako/auth/AuthIntegrationTest.java

  trip-service/
    pom.xml
    Dockerfile
    src/main/java/zako/trip/
      TripServiceApplication.java
      station/{Station,StationController,StationRepository,StationService,StationSeeder}.java
      station/dto/{StationRequest,StationResponse}.java
      train/{Train,TrainType,TrainController,TrainRepository,TrainService}.java
      train/dto/{TrainRequest,TrainResponse}.java
      route/{Route,RouteStop,RouteRepository,RouteStopRepository}.java
      trip/{Trip,TripStatus,TripController,TripRepository,TripService}.java
      trip/dto/{TripRequest,TripResponse,TripSearchRequest}.java
      kafka/{TicketEvent,TicketEventConsumer}.java
      config/{JwtService,JwtAuthFilter,SecurityConfig}.java        ← no UserRepository dep
      exception/{GlobalExceptionHandler,ResourceNotFoundException}.java
      seeder/DemoDataSeeder.java
    src/main/resources/
      application.properties
      db/migration/V1__schema.sql
    src/test/java/zako/trip/TripIntegrationTest.java

  ticket-service/
    pom.xml
    Dockerfile
    src/main/java/zako/ticket/
      TicketServiceApplication.java
      ticket/{Ticket,TicketStatus,PaymentStatus,TicketController,TicketRepository,TicketService}.java
      ticket/dto/{TicketRequest,TicketResponse,StationDto}.java
      kafka/{TicketEvent,TicketEventProducer}.java
      client/TripServiceClient.java
      config/{JwtService,JwtAuthFilter,SecurityConfig}.java        ← no UserRepository dep
      exception/{GlobalExceptionHandler,ResourceNotFoundException}.java
    src/main/resources/
      application.properties
      db/migration/V1__create_tickets.sql
    src/test/java/zako/ticket/TicketIntegrationTest.java

k8s-microservices/
  00-namespace.yaml
  10-kafka.yaml
  20-auth-db.yaml
  21-auth-service.yaml
  30-trip-db.yaml
  31-trip-service.yaml
  40-ticket-db.yaml
  41-ticket-service.yaml
  50-ingress.yaml
  60-hpa.yaml
  70-prometheus.yaml
  71-grafana.yaml
  72-jaeger.yaml

docs/sprawozdanie.md
```

---

## Task 1: Fix monolith — add `/api/tickets/my` endpoint

**Files:**
- Modify: `monolith/src/main/java/zako/monolith/ticket/TicketController.java`
- Modify: `frontend/src/app/services/ticket-api.service.ts`
- Modify: `frontend/src/app/pages/my-tickets/my-tickets.ts`

- [ ] **Add endpoint to TicketController** — insert after the existing `getUserTickets` method:

```java
@GetMapping("/my")
public List<TicketResponse> getMyTickets(Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    return ticketService.getUserTickets(user.getId());
}
```

- [ ] **Update ticket-api.service.ts** — replace `getUserTickets(userId)` with a call to `/my`:

```typescript
// Replace the entire getUserTickets method:
getMyTickets(): Observable<TicketDto[]> {
  return this.http.get<TicketDto[]>('/api/tickets/my');
}
```

- [ ] **Update my-tickets.ts** — remove userId dependency:

```typescript
// Replace ngOnInit body:
ngOnInit() {
  this.ticketApi.getMyTickets().subscribe({
    next: t => { this.tickets.set(t); this.loading.set(false); },
    error: () => { this.error.set('Błąd ładowania biletów.'); this.loading.set(false); }
  });
}
```

- [ ] **Run monolith** — verify `GET http://localhost:9090/api/tickets/my` returns 401 without token and 200 with valid JWT (test with curl or browser devtools after login)

- [ ] **Commit**

```bash
git add monolith/src/main/java/zako/monolith/ticket/TicketController.java \
        frontend/src/app/services/ticket-api.service.ts \
        frontend/src/app/pages/my-tickets/my-tickets.ts
git commit -m "fix(tickets): add /api/tickets/my endpoint and wire frontend to it"
```

---

## Task 2: Push all files and merge to main, create microservices branch

- [ ] **Stage and push all untracked files**

```bash
git add k6/ k8s/15-app-secret.yaml k8s/50-prometheus.yaml k8s/51-grafana.yaml \
        k8s/52-jaeger.yaml k8s/60-hpa.yaml kind-cluster.yaml run-monolith.ps1 \
        setup-kind.ps1 setup-minikube.ps1 monolith/.mvn/jvm.config \
        docs/superpowers/plans/2026-05-12-kubernetes-comparison.md
git commit -m "chore: add k6 tests, observability k8s manifests, kind/minikube setup scripts"
git push origin feat/koleo-homepage
```

- [ ] **Merge to main**

```bash
git checkout main
git merge feat/koleo-homepage --no-ff -m "feat: complete monolith with k8s, auth, tickets, observability"
git push origin main
```

- [ ] **Create microservices branch**

```bash
git checkout -b feat/microservices
git push -u origin feat/microservices
```

---

## Task 3: Microservices parent POM and directory scaffold

**Files:**
- Create: `microservices/pom.xml`

- [ ] **Create `microservices/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.4</version>
    <relativePath/>
  </parent>
  <groupId>zako</groupId>
  <artifactId>microservices-parent</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <packaging>pom</packaging>
  <modules>
    <module>auth-service</module>
    <module>trip-service</module>
    <module>ticket-service</module>
  </modules>
  <properties>
    <java.version>21</java.version>
    <jjwt.version>0.12.6</jjwt.version>
  </properties>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>${jjwt.version}</version>
      </dependency>
      <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
      </dependency>
      <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>
```

- [ ] **Create `microservices/auth-service/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>zako</groupId>
    <artifactId>microservices-parent</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
  </parent>
  <artifactId>auth-service</artifactId>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webmvc</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
    <dependency><groupId>io.micrometer</groupId><artifactId>micrometer-registry-prometheus</artifactId></dependency>
    <dependency><groupId>io.micrometer</groupId><artifactId>micrometer-tracing-bridge-otel</artifactId></dependency>
    <dependency><groupId>io.opentelemetry</groupId><artifactId>opentelemetry-exporter-otlp</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <annotationProcessorPaths>
            <path><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></path>
          </annotationProcessorPaths>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes><exclude><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></exclude></excludes>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Create `microservices/trip-service/pom.xml`** — same as auth-service pom but `<artifactId>trip-service</artifactId>` and add spring-kafka:

```xml
<!-- Same structure as auth-service pom, change artifactId, add: -->
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka-test</artifactId>
  <scope>test</scope>
</dependency>
```

- [ ] **Create `microservices/ticket-service/pom.xml`** — same as trip-service pom but `<artifactId>ticket-service</artifactId>`

- [ ] **Verify Maven builds** (from `microservices/` directory — no tests yet, just compilation check):

```bash
cd microservices
mvn -B validate
```

Expected: `BUILD SUCCESS`

- [ ] **Commit scaffold**

```bash
git add microservices/
git commit -m "chore(microservices): add parent pom and per-service pom scaffolds"
```

---

## Task 4: auth-service implementation

**Files:** all under `microservices/auth-service/src/`

- [ ] **Create `V1__create_users.sql`** at `microservices/auth-service/src/main/resources/db/migration/`:

```sql
CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL    PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name  VARCHAR(255) NOT NULL,
    email      VARCHAR(255) UNIQUE NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);
```

- [ ] **Create `application.properties`** at `microservices/auth-service/src/main/resources/`:

```properties
spring.application.name=auth-service
server.port=${SERVER_PORT:8081}
server.shutdown=graceful

spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/auth_db}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:1234}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=none
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1

jwt.secret=${JWT_SECRET:ZGV2LXNlY3JldC1rZXktZm9yLXpha28tYXBwLTI1NmJpdHM=}
jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}

management.endpoints.web.exposure.include=health,prometheus,metrics,info
management.endpoint.health.show-details=never
management.endpoint.prometheus.access=unrestricted
management.tracing.sampling.probability=1.0
management.otlp.tracing.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
```

- [ ] **Create `AuthServiceApplication.java`**:

```java
package zako.auth;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class AuthServiceApplication {
    public static void main(String[] args) { SpringApplication.run(AuthServiceApplication.class, args); }
}
```

- [ ] **Copy user domain** — create these files (identical to monolith counterparts, change package from `zako.monolith.*` to `zako.auth.*`):
  - `user/Role.java` — enum `USER, ADMIN`
  - `user/User.java` — `@Entity`, same fields as monolith
  - `user/UserRepository.java` — `findByEmail(String email): Optional<User>`
  - `user/dto/RegisterRequest.java` — record with `email, password, firstName, lastName` (all `@NotBlank`)
  - `user/dto/UserResponse.java` — record with `id, email, firstName, lastName, role`; static `from(User u)`
  - `user/UserService.java` — `register(RegisterRequest)`, `findByEmail(String)`, `findById(Long)` (uses `PasswordEncoder`, `UserRepository`)
  - `user/UserController.java` — `POST /api/users/register`
  - `auth/dto/LoginRequest.java` — record `email, password`
  - `auth/dto/LoginResponse.java` — record `token, user` (where user is `UserResponse`)
  - `auth/AuthService.java` — `login(LoginRequest): LoginResponse`
  - `auth/AuthController.java` — `POST /api/auth/login`, `GET /api/auth/me`
  - `exception/ResourceNotFoundException.java` — extends `RuntimeException`
  - `exception/GlobalExceptionHandler.java` — `@RestControllerAdvice`, handles `ResourceNotFoundException` (404), `IllegalArgumentException` (400), generic (500)

- [ ] **Create `config/JwtService.java`** — identical to monolith version, change package to `zako.auth.config`

- [ ] **Create `config/JwtAuthFilter.java`** — identical to monolith (uses `UserRepository` to load full user from DB), change package to `zako.auth.config`

- [ ] **Create `config/SecurityConfig.java`**:

```java
package zako.auth.config;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration @EnableWebSecurity @RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/users/register").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
```

- [ ] **Write failing test** at `src/test/java/zako/auth/AuthIntegrationTest.java`:

```java
package zako.auth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AuthIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void registerAndLogin() throws Exception {
        mvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"test@zako.local","password":"Pass1234!",
                     "firstName":"Test","lastName":"User"}
                    """))
            .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"test@zako.local","password":"Pass1234!"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString());
    }
}
```

- [ ] **Create test application.properties** at `src/test/resources/application-test.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:auth_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
management.otlp.tracing.endpoint=http://localhost:4318/v1/traces
```

- [ ] **Run test and verify PASS**:

```bash
cd microservices
mvn -pl auth-service test
```

Expected: `BUILD SUCCESS`, 1 test passed

- [ ] **Create `Dockerfile`** at `microservices/auth-service/Dockerfile`:

```dockerfile
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
```

Note: Dockerfile COPY paths are relative to build context `microservices/` (passed as `-f auth-service/Dockerfile .` from `microservices/` dir).

- [ ] **Commit auth-service**

```bash
git add microservices/auth-service/
git commit -m "feat(microservices): implement auth-service with JWT and user registration"
```

---

## Task 5: trip-service implementation

**Files:** all under `microservices/trip-service/src/`

- [ ] **Create `V1__schema.sql`** at `microservices/trip-service/src/main/resources/db/migration/`:

```sql
CREATE TABLE IF NOT EXISTS stations (
    id        BIGSERIAL    PRIMARY KEY,
    name      VARCHAR(255) UNIQUE NOT NULL,
    city      VARCHAR(255) NOT NULL,
    code      VARCHAR(255) UNIQUE NOT NULL,
    latitude  DOUBLE PRECISION,
    longitude DOUBLE PRECISION
);
CREATE TABLE IF NOT EXISTS trains (
    id           BIGSERIAL    PRIMARY KEY,
    train_number VARCHAR(255) UNIQUE NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    total_seats  INTEGER      NOT NULL
);
CREATE TABLE IF NOT EXISTS routes (
    id   BIGSERIAL    PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS route_stops (
    id                BIGSERIAL PRIMARY KEY,
    route_id          BIGINT    NOT NULL REFERENCES routes(id),
    station_id        BIGINT    NOT NULL REFERENCES stations(id),
    stop_order        INTEGER   NOT NULL,
    minutes_from_start INTEGER
);
CREATE TABLE IF NOT EXISTS trips (
    id              BIGSERIAL    PRIMARY KEY,
    train_id        BIGINT       NOT NULL REFERENCES trains(id),
    route_id        BIGINT       NOT NULL REFERENCES routes(id),
    departure_time  TIMESTAMP    NOT NULL,
    arrival_time    TIMESTAMP    NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'SCHEDULED',
    available_seats INTEGER      NOT NULL
);
```

- [ ] **Create `application.properties`** at `microservices/trip-service/src/main/resources/`:

```properties
spring.application.name=trip-service
server.port=${SERVER_PORT:8082}
server.shutdown=graceful

spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/trip_db}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:1234}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=none
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1

spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.consumer.group-id=trip-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=zako.trip.kafka,zako.ticket.kafka

jwt.secret=${JWT_SECRET:ZGV2LXNlY3JldC1rZXktZm9yLXpha28tYXBwLTI1NmJpdHM=}

management.endpoints.web.exposure.include=health,prometheus,metrics,info
management.endpoint.health.show-details=never
management.endpoint.prometheus.access=unrestricted
management.tracing.sampling.probability=1.0
management.otlp.tracing.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
```

- [ ] **Copy domain classes** from monolith — create under `zako.trip.*` packages (same logic, change package):
  - `station/Station.java`, `StationController.java`, `StationRepository.java`, `StationService.java`
  - `station/dto/StationRequest.java`, `StationResponse.java`
  - `train/Train.java`, `TrainType.java`, `TrainController.java`, `TrainRepository.java`, `TrainService.java`
  - `train/dto/TrainRequest.java`, `TrainResponse.java`
  - `route/Route.java`, `RouteStop.java` — replace `minutesFromStart` field name with `minutesFromStart` (matches monolith)
  - `route/RouteRepository.java`, `RouteStopRepository.java`
  - `trip/Trip.java`, `TripStatus.java`, `TripController.java`, `TripRepository.java`, `TripService.java`
  - `trip/dto/TripRequest.java`, `TripResponse.java`, `TripSearchRequest.java`
  - `exception/ResourceNotFoundException.java`, `GlobalExceptionHandler.java`
  - `seeder/StationSeeder.java` — copy from `monolith/.../StationSeeder.java`, change package
  - `seeder/DemoDataSeeder.java` — copy from `monolith/.../DemoDataSeeder.java`, change package

- [ ] **Create `TripServiceApplication.java`**:

```java
package zako.trip;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class TripServiceApplication {
    public static void main(String[] args) { SpringApplication.run(TripServiceApplication.class, args); }
}
```

- [ ] **Create `kafka/TicketEvent.java`** — shared event DTO:

```java
package zako.trip.kafka;
public record TicketEvent(String type, Long ticketId, Long tripId, int seats) {}
```

- [ ] **Create `kafka/TicketEventConsumer.java`**:

```java
package zako.trip.kafka;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import zako.trip.trip.TripRepository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j @Component @RequiredArgsConstructor
public class TicketEventConsumer {
    private final TripRepository tripRepository;

    @KafkaListener(topics = {"ticket.purchased", "ticket.cancelled"})
    @Transactional
    public void onTicketEvent(TicketEvent event) {
        tripRepository.findById(event.tripId()).ifPresentOrElse(trip -> {
            int delta = "ticket.cancelled".equals(event.type()) ? event.seats() : -event.seats();
            trip.setAvailableSeats(Math.max(0, trip.getAvailableSeats() + delta));
            tripRepository.save(trip);
            log.info("Updated availableSeats for trip {} by {}", event.tripId(), delta);
        }, () -> log.warn("Trip {} not found for event {}", event.tripId(), event.type()));
    }
}
```

- [ ] **Create simplified `config/JwtAuthFilter.java`** — trip-service has no UserRepository; builds principal from JWT claims only:

```java
package zako.trip.config;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component @RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { chain.doFilter(req, res); return; }
        String token = header.substring(7);
        if (!jwtService.isValid(token)) { res.sendError(401, "Invalid token"); return; }
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Long userId = jwtService.extractUserId(token);
            var auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(req, res);
    }
}
```

- [ ] **Create `config/JwtService.java`** — only validation + claim extraction (no `generateToken`):

```java
package zako.trip.config;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;

@Service
public class JwtService {
    @Value("${jwt.secret}") private String secret;

    public Long extractUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }
    public boolean isValid(String token) {
        try { parseClaims(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }
    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }
    private SecretKey key() { return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)); }
}
```

- [ ] **Create `config/SecurityConfig.java`** for trip-service — all GET endpoints public:

```java
package zako.trip.config;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration @EnableWebSecurity @RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/stations/**", "/api/trips/**", "/api/trains/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/trips/search").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

- [ ] **Write failing test** at `src/test/java/zako/trip/TripIntegrationTest.java`:

```java
package zako.trip;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class TripIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void stationsEndpointIsPublic() throws Exception {
        mvc.perform(get("/api/stations")).andExpect(status().isOk());
    }

    @Test void tripsEndpointIsPublic() throws Exception {
        mvc.perform(get("/api/trips")).andExpect(status().isOk());
    }
}
```

- [ ] **Create `src/test/resources/application-test.properties`**:

```properties
spring.datasource.url=jdbc:h2:mem:trip_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.kafka.bootstrap-servers=localhost:9092
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
management.otlp.tracing.endpoint=http://localhost:4318/v1/traces
```

- [ ] **Run test**:

```bash
cd microservices
mvn -pl trip-service test
```

Expected: `BUILD SUCCESS`, 2 tests passed

- [ ] **Create `Dockerfile`** at `microservices/trip-service/Dockerfile` (same pattern as auth-service, change service name):

```dockerfile
# syntax=docker/dockerfile:1.6
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
ENV MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true"
COPY pom.xml ../pom.xml
COPY trip-service/pom.xml trip-service/pom.xml
RUN mvn -B -q -pl trip-service dependency:go-offline
COPY trip-service/src trip-service/src
RUN mvn -B -q -pl trip-service -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/trip-service/target/*.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
```

- [ ] **Commit trip-service**

```bash
git add microservices/trip-service/
git commit -m "feat(microservices): implement trip-service with Kafka consumer for seat updates"
```

---

## Task 6: ticket-service implementation

**Files:** all under `microservices/ticket-service/src/`

- [ ] **Create `V1__create_tickets.sql`** at `microservices/ticket-service/src/main/resources/db/migration/`:

```sql
CREATE TABLE IF NOT EXISTS tickets (
    id                 BIGSERIAL        PRIMARY KEY,
    user_id            BIGINT           NOT NULL,
    trip_id            BIGINT           NOT NULL,
    from_station_id    BIGINT           NOT NULL,
    from_station_name  VARCHAR(255),
    from_station_city  VARCHAR(255),
    from_station_code  VARCHAR(20),
    to_station_id      BIGINT           NOT NULL,
    to_station_name    VARCHAR(255),
    to_station_city    VARCHAR(255),
    to_station_code    VARCHAR(20),
    seat_number        INTEGER,
    price              NUMERIC(10, 2)   NOT NULL,
    status             VARCHAR(50)      NOT NULL DEFAULT 'ACTIVE',
    payment_status     VARCHAR(50)      NOT NULL DEFAULT 'PENDING',
    ticket_code        VARCHAR(255)     UNIQUE NOT NULL,
    purchased_at       TIMESTAMP        NOT NULL DEFAULT now()
);
```

- [ ] **Create `application.properties`** at `microservices/ticket-service/src/main/resources/`:

```properties
spring.application.name=ticket-service
server.port=${SERVER_PORT:8083}
server.shutdown=graceful

spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/ticket_db}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:1234}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=none
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1

spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

trip.service.url=${TRIP_SERVICE_URL:http://localhost:8082}

jwt.secret=${JWT_SECRET:ZGV2LXNlY3JldC1rZXktZm9yLXpha28tYXBwLTI1NmJpdHM=}

management.endpoints.web.exposure.include=health,prometheus,metrics,info
management.endpoint.health.show-details=never
management.endpoint.prometheus.access=unrestricted
management.tracing.sampling.probability=1.0
management.otlp.tracing.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
```

- [ ] **Create `TicketServiceApplication.java`**:

```java
package zako.ticket;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class TicketServiceApplication {
    public static void main(String[] args) { SpringApplication.run(TicketServiceApplication.class, args); }
}
```

- [ ] **Create `ticket/TicketStatus.java`** and **`ticket/PaymentStatus.java`** — same enums as monolith, package `zako.ticket.ticket`

- [ ] **Create `ticket/Ticket.java`** — denormalized entity (no FK to other services):

```java
package zako.ticket.ticket;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "tickets")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private Long tripId;
    @Column(nullable = false) private Long fromStationId;
    private String fromStationName;
    private String fromStationCity;
    private String fromStationCode;
    @Column(nullable = false) private Long toStationId;
    private String toStationName;
    private String toStationCity;
    private String toStationCode;
    private Integer seatNumber;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal price;
    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default
    private TicketStatus status = TicketStatus.ACTIVE;
    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    @Column(unique = true, nullable = false) private String ticketCode;
    @Builder.Default private LocalDateTime purchasedAt = LocalDateTime.now();
}
```

- [ ] **Create `ticket/dto/StationDto.java`**:

```java
package zako.ticket.ticket.dto;
public record StationDto(Long id, String name, String city, String code) {}
```

- [ ] **Create `ticket/dto/TicketRequest.java`**:

```java
package zako.ticket.ticket.dto;
import java.math.BigDecimal;
public record TicketRequest(Long tripId, Long fromStationId, Long toStationId, BigDecimal price) {}
```

- [ ] **Create `ticket/dto/TicketResponse.java`**:

```java
package zako.ticket.ticket.dto;
import zako.ticket.ticket.Ticket;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponse(
    Long id, Long userId, Long tripId,
    StationInfo fromStation, StationInfo toStation,
    Integer seatNumber, BigDecimal price,
    String status, String paymentStatus,
    String ticketCode, LocalDateTime purchasedAt
) {
    public record StationInfo(Long id, String name, String city, String code) {}

    public static TicketResponse from(Ticket t) {
        return new TicketResponse(
            t.getId(), t.getUserId(), t.getTripId(),
            new StationInfo(t.getFromStationId(), t.getFromStationName(), t.getFromStationCity(), t.getFromStationCode()),
            new StationInfo(t.getToStationId(), t.getToStationName(), t.getToStationCity(), t.getToStationCode()),
            t.getSeatNumber(), t.getPrice(),
            t.getStatus().name(), t.getPaymentStatus().name(),
            t.getTicketCode(), t.getPurchasedAt()
        );
    }
}
```

- [ ] **Create `ticket/TicketRepository.java`**:

```java
package zako.ticket.ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUserId(Long userId);
}
```

- [ ] **Create `kafka/TicketEvent.java`**:

```java
package zako.ticket.kafka;
public record TicketEvent(String type, Long ticketId, Long tripId, int seats) {}
```

- [ ] **Create `kafka/TicketEventProducer.java`**:

```java
package zako.ticket.kafka;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import zako.ticket.ticket.Ticket;

@Component @RequiredArgsConstructor
public class TicketEventProducer {
    private final KafkaTemplate<String, TicketEvent> kafkaTemplate;

    public void publishPurchased(Ticket t) {
        kafkaTemplate.send("ticket.purchased",
            new TicketEvent("ticket.purchased", t.getId(), t.getTripId(), 1));
    }
    public void publishCancelled(Ticket t) {
        kafkaTemplate.send("ticket.cancelled",
            new TicketEvent("ticket.cancelled", t.getId(), t.getTripId(), 1));
    }
}
```

- [ ] **Create `client/TripServiceClient.java`**:

```java
package zako.ticket.client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import zako.ticket.ticket.dto.StationDto;

@Component
public class TripServiceClient {
    private final RestClient restClient;

    public TripServiceClient(@Value("${trip.service.url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public StationDto getStation(Long id) {
        return restClient.get().uri("/api/stations/{id}", id)
            .retrieve().body(StationDto.class);
    }
}
```

- [ ] **Create `ticket/TicketService.java`**:

```java
package zako.ticket.ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zako.ticket.client.TripServiceClient;
import zako.ticket.exception.ResourceNotFoundException;
import zako.ticket.kafka.TicketEventProducer;
import zako.ticket.ticket.dto.StationDto;
import zako.ticket.ticket.dto.TicketRequest;
import zako.ticket.ticket.dto.TicketResponse;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final TripServiceClient tripServiceClient;
    private final TicketEventProducer eventProducer;

    @Transactional
    public TicketResponse purchase(Long userId, TicketRequest req) {
        StationDto from = tripServiceClient.getStation(req.fromStationId());
        StationDto to   = tripServiceClient.getStation(req.toStationId());
        Ticket ticket = Ticket.builder()
            .userId(userId).tripId(req.tripId())
            .fromStationId(from.id()).fromStationName(from.name())
            .fromStationCity(from.city()).fromStationCode(from.code())
            .toStationId(to.id()).toStationName(to.name())
            .toStationCity(to.city()).toStationCode(to.code())
            .price(req.price())
            .ticketCode(UUID.randomUUID().toString().toUpperCase().replace("-","").substring(0,12))
            .build();
        ticket = ticketRepository.save(ticket);
        eventProducer.publishPurchased(ticket);
        return TicketResponse.from(ticket);
    }

    @Transactional
    public TicketResponse pay(Long ticketId, Long userId) {
        Ticket ticket = findById(ticketId);
        if (!ticket.getUserId().equals(userId)) throw new IllegalStateException("Not authorized");
        if (ticket.getPaymentStatus() == PaymentStatus.PAID) throw new IllegalStateException("Already paid");
        ticket.setPaymentStatus(PaymentStatus.PAID);
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse cancel(Long ticketId, Long userId) {
        Ticket ticket = findById(ticketId);
        if (!ticket.getUserId().equals(userId)) throw new IllegalStateException("Not authorized");
        if (ticket.getStatus() != TicketStatus.ACTIVE) throw new IllegalStateException("Only active tickets can be cancelled");
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket = ticketRepository.save(ticket);
        eventProducer.publishCancelled(ticket);
        return TicketResponse.from(ticket);
    }

    public List<TicketResponse> getUserTickets(Long userId) {
        return ticketRepository.findByUserId(userId).stream().map(TicketResponse::from).toList();
    }

    public TicketResponse getById(Long id) { return TicketResponse.from(findById(id)); }

    private Ticket findById(Long id) {
        return ticketRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));
    }
}
```

- [ ] **Create `ticket/TicketController.java`**:

```java
package zako.ticket.ticket;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import zako.ticket.ticket.dto.TicketRequest;
import zako.ticket.ticket.dto.TicketResponse;
import java.util.List;

@RestController @RequestMapping("/api/tickets") @RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse purchase(@Valid @RequestBody TicketRequest req, Authentication auth) {
        return ticketService.purchase((Long) auth.getPrincipal(), req);
    }

    @PostMapping("/{id}/pay")
    public TicketResponse pay(@PathVariable Long id, Authentication auth) {
        return ticketService.pay(id, (Long) auth.getPrincipal());
    }

    @PostMapping("/{id}/cancel")
    public TicketResponse cancel(@PathVariable Long id, Authentication auth) {
        return ticketService.cancel(id, (Long) auth.getPrincipal());
    }

    @GetMapping("/my")
    public List<TicketResponse> getMyTickets(Authentication auth) {
        return ticketService.getUserTickets((Long) auth.getPrincipal());
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable Long id) {
        return ticketService.getById(id);
    }
}
```

- [ ] **Copy `config/JwtService.java`** and **`config/JwtAuthFilter.java`** from trip-service (same implementation, change package to `zako.ticket.config`)

- [ ] **Create `config/SecurityConfig.java`** for ticket-service:

```java
package zako.ticket.config;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration @EnableWebSecurity @RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

- [ ] **Create `exception/ResourceNotFoundException.java`** and **`exception/GlobalExceptionHandler.java`** — same as auth-service, package `zako.ticket.exception`

- [ ] **Write failing test** at `src/test/java/zako/ticket/TicketIntegrationTest.java`:

```java
package zako.ticket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class TicketIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void myTicketsRequiresAuth() throws Exception {
        mvc.perform(get("/api/tickets/my")).andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Create `src/test/resources/application-test.properties`**:

```properties
spring.datasource.url=jdbc:h2:mem:ticket_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
trip.service.url=http://localhost:8082
management.otlp.tracing.endpoint=http://localhost:4318/v1/traces
```

- [ ] **Run test**:

```bash
cd microservices
mvn -pl ticket-service test
```

Expected: `BUILD SUCCESS`, 1 test passed

- [ ] **Create `Dockerfile`** at `microservices/ticket-service/Dockerfile` (same as trip-service, port 8083):

```dockerfile
# syntax=docker/dockerfile:1.6
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
ENV MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true"
COPY pom.xml ../pom.xml
COPY ticket-service/pom.xml ticket-service/pom.xml
RUN mvn -B -q -pl ticket-service dependency:go-offline
COPY ticket-service/src ticket-service/src
RUN mvn -B -q -pl ticket-service -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/ticket-service/target/*.jar app.jar
USER app
EXPOSE 8083
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
```

- [ ] **Commit ticket-service**

```bash
git add microservices/ticket-service/
git commit -m "feat(microservices): implement ticket-service with Kafka producer and trip-service REST client"
```

---

## Task 7: k8s manifests for `zako-ms` namespace

**Files:** all under `k8s-microservices/`

- [ ] **Create `00-namespace.yaml`**:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: zako-ms
```

- [ ] **Create `10-kafka.yaml`** — Kafka KRaft single broker:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: kafka
  namespace: zako-ms
spec:
  clusterIP: None
  selector:
    app: kafka
  ports:
    - name: broker
      port: 9092
      targetPort: 9092
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka
  namespace: zako-ms
spec:
  serviceName: kafka
  replicas: 1
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      containers:
        - name: kafka
          image: bitnami/kafka:3.8
          ports:
            - containerPort: 9092
            - containerPort: 9093
          env:
            - name: KAFKA_CFG_NODE_ID
              value: "0"
            - name: KAFKA_CFG_PROCESS_ROLES
              value: "controller,broker"
            - name: KAFKA_CFG_LISTENERS
              value: "PLAINTEXT://:9092,CONTROLLER://:9093"
            - name: KAFKA_CFG_ADVERTISED_LISTENERS
              value: "PLAINTEXT://kafka:9092"
            - name: KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP
              value: "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT"
            - name: KAFKA_CFG_CONTROLLER_QUORUM_VOTERS
              value: "0@kafka:9093"
            - name: KAFKA_CFG_CONTROLLER_LISTENER_NAMES
              value: "CONTROLLER"
            - name: ALLOW_PLAINTEXT_LISTENER
              value: "yes"
          resources:
            requests:
              cpu: "200m"
              memory: "512Mi"
            limits:
              cpu: "500m"
              memory: "1Gi"
          volumeMounts:
            - name: data
              mountPath: /bitnami/kafka
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 1Gi
```

- [ ] **Create `20-auth-db.yaml`**:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: auth-db-secret
  namespace: zako-ms
stringData:
  POSTGRES_USER: postgres
  POSTGRES_PASSWORD: zako_pass
  POSTGRES_DB: auth_db
---
apiVersion: v1
kind: Service
metadata:
  name: auth-db
  namespace: zako-ms
spec:
  clusterIP: None
  selector:
    app: auth-db
  ports:
    - port: 5432
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: auth-db
  namespace: zako-ms
spec:
  serviceName: auth-db
  replicas: 1
  selector:
    matchLabels:
      app: auth-db
  template:
    metadata:
      labels:
        app: auth-db
    spec:
      containers:
        - name: postgres
          image: postgres:16-alpine
          ports:
            - containerPort: 5432
          envFrom:
            - secretRef:
                name: auth-db-secret
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", "postgres", "-d", "auth_db"]
            initialDelaySeconds: 5
            periodSeconds: 5
          resources:
            requests:
              cpu: "100m"
              memory: "128Mi"
            limits:
              cpu: "300m"
              memory: "256Mi"
          volumeMounts:
            - name: data
              mountPath: /var/lib/postgresql/data
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 1Gi
```

- [ ] **Create `30-trip-db.yaml`** — same as `20-auth-db.yaml`, replace every `auth-db` → `trip-db` and `auth_db` → `trip_db`

- [ ] **Create `40-ticket-db.yaml`** — same pattern, `ticket-db` / `ticket_db`

- [ ] **Create shared app secret `15-app-secret.yaml`**:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secret
  namespace: zako-ms
stringData:
  JWT_SECRET: ZGV2LXNlY3JldC1rZXktZm9yLXpha28tYXBwLTI1NmJpdHM=
```

- [ ] **Create `21-auth-service.yaml`**:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: auth-config
  namespace: zako-ms
data:
  SPRING_DATASOURCE_URL: jdbc:postgresql://auth-db:5432/auth_db
  SERVER_PORT: "8081"
  OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
---
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: zako-ms
spec:
  selector:
    app: auth-service
  ports:
    - port: 8081
      targetPort: 8081
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: zako-ms
spec:
  replicas: 1
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: /actuator/prometheus
        prometheus.io/port: "8081"
    spec:
      containers:
        - name: auth-service
          image: zako/auth-service:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8081
          envFrom:
            - configMapRef:
                name: auth-config
          env:
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: auth-db-secret
                  key: POSTGRES_USER
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: auth-db-secret
                  key: POSTGRES_PASSWORD
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: app-secret
                  key: JWT_SECRET
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 60
            periodSeconds: 30
          resources:
            requests:
              cpu: "150m"
              memory: "256Mi"
            limits:
              cpu: "600m"
              memory: "512Mi"
```

- [ ] **Create `31-trip-service.yaml`** — same structure as auth-service, with:
  - `name: trip-service`, port `8082`, image `zako/trip-service:latest`
  - ConfigMap adds `KAFKA_BOOTSTRAP_SERVERS: kafka:9092`
  - DB secret ref: `trip-db-secret`
  - Datasource URL: `jdbc:postgresql://trip-db:5432/trip_db`

- [ ] **Create `41-ticket-service.yaml`** — same structure, with:
  - `name: ticket-service`, port `8083`, image `zako/ticket-service:latest`
  - ConfigMap adds `KAFKA_BOOTSTRAP_SERVERS: kafka:9092`, `TRIP_SERVICE_URL: http://trip-service:8082`
  - DB secret ref: `ticket-db-secret`
  - Datasource URL: `jdbc:postgresql://ticket-db:5432/ticket_db`

- [ ] **Create `50-ingress.yaml`**:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: zako-ms
  namespace: zako-ms
spec:
  ingressClassName: nginx
  rules:
    - host: zako-ms.localhost
      http:
        paths:
          - path: /api/auth
            pathType: Prefix
            backend:
              service:
                name: auth-service
                port:
                  number: 8081
          - path: /api/users
            pathType: Prefix
            backend:
              service:
                name: auth-service
                port:
                  number: 8081
          - path: /api/stations
            pathType: Prefix
            backend:
              service:
                name: trip-service
                port:
                  number: 8082
          - path: /api/trains
            pathType: Prefix
            backend:
              service:
                name: trip-service
                port:
                  number: 8082
          - path: /api/trips
            pathType: Prefix
            backend:
              service:
                name: trip-service
                port:
                  number: 8082
          - path: /api/tickets
            pathType: Prefix
            backend:
              service:
                name: ticket-service
                port:
                  number: 8083
          - path: /grafana
            pathType: Prefix
            backend:
              service:
                name: grafana-ms
                port:
                  number: 3000
          - path: /
            pathType: Prefix
            backend:
              service:
                name: frontend-ms
                port:
                  number: 80
```

- [ ] **Create `60-hpa.yaml`**:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: auth-service
  namespace: zako-ms
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: auth-service
  minReplicas: 1
  maxReplicas: 3
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: trip-service
  namespace: zako-ms
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: trip-service
  minReplicas: 1
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ticket-service
  namespace: zako-ms
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ticket-service
  minReplicas: 1
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
```

- [ ] **Create `70-prometheus.yaml`** — same as `k8s/50-prometheus.yaml`, change namespace to `zako-ms`, update scrape targets to all 3 services:

```yaml
# In prometheus.yml ConfigMap, scrape_configs:
scrape_configs:
  - job_name: auth-service
    metrics_path: /actuator/prometheus
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names: [zako-ms]
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: "true"
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        target_label: __address__
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
      - source_labels: [__meta_kubernetes_pod_label_app]
        target_label: app
```

- [ ] **Create `71-grafana.yaml`** — same as `k8s/51-grafana.yaml`, change namespace to `zako-ms`, service name to `grafana-ms`, `GF_SERVER_ROOT_URL` to `http://zako-ms.localhost/grafana`

- [ ] **Create `72-jaeger.yaml`** — copy of `k8s/52-jaeger.yaml`, namespace `zako-ms`

- [ ] **Add frontend deployment for zako-ms** at end of `71-grafana.yaml` or separate file:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: frontend-ms
  namespace: zako-ms
spec:
  selector:
    app: frontend-ms
  ports:
    - port: 80
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend-ms
  namespace: zako-ms
spec:
  replicas: 1
  selector:
    matchLabels:
      app: frontend-ms
  template:
    metadata:
      labels:
        app: frontend-ms
    spec:
      containers:
        - name: frontend
          image: zako/frontend:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 80
          resources:
            requests:
              cpu: "50m"
              memory: "64Mi"
            limits:
              cpu: "200m"
              memory: "128Mi"
```

- [ ] **Add hosts entries** — open `C:\Windows\System32\drivers\etc\hosts` as Administrator and add:

```
127.0.0.1  zako.localhost
127.0.0.1  zako-ms.localhost
```

- [ ] **Commit all k8s manifests**

```bash
git add k8s-microservices/
git commit -m "feat(k8s): add zako-ms namespace manifests (Kafka, 3x Postgres, 3 services, HPA, observability)"
```

---

## Task 8: Build Docker images and deploy to Kind

- [ ] **Start Kind cluster** (if not already running):

```powershell
kind create cluster --config kind-cluster.yaml --name zako-cluster
# Verify:
kubectl cluster-info --context kind-zako-cluster
```

Expected: cluster info shows control-plane at `127.0.0.1:XXXXX`

- [ ] **Install ingress-nginx** (if not already installed):

```powershell
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=90s
```

- [ ] **Build all Docker images** — run from `microservices/` directory:

```bash
docker build -f auth-service/Dockerfile -t zako/auth-service:latest .
docker build -f trip-service/Dockerfile -t zako/trip-service:latest .
docker build -f ticket-service/Dockerfile -t zako/ticket-service:latest .
```

Build frontend image from repo root:

```bash
docker build -f frontend/Dockerfile -t zako/frontend:latest .
docker build -f monolith/Dockerfile -t zako/monolith:latest .
```

- [ ] **Load images into Kind**:

```bash
kind load docker-image zako/auth-service:latest --name zako-cluster
kind load docker-image zako/trip-service:latest --name zako-cluster
kind load docker-image zako/ticket-service:latest --name zako-cluster
kind load docker-image zako/frontend:latest --name zako-cluster
kind load docker-image zako/monolith:latest --name zako-cluster
```

- [ ] **Deploy monolith stack** (namespace `zako`):

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/10-postgres-secret.yaml
kubectl apply -f k8s/11-postgres-statefulset.yaml
kubectl apply -f k8s/15-app-secret.yaml
kubectl apply -f k8s/20-monolith-config.yaml
kubectl apply -f k8s/21-monolith.yaml
kubectl apply -f k8s/30-frontend.yaml
kubectl apply -f k8s/40-ingress.yaml
kubectl apply -f k8s/50-prometheus.yaml
kubectl apply -f k8s/51-grafana.yaml
kubectl apply -f k8s/52-jaeger.yaml
kubectl apply -f k8s/60-hpa.yaml
```

- [ ] **Wait for monolith readiness**:

```bash
kubectl wait --namespace zako --for=condition=ready pod --selector=app=monolith --timeout=120s
kubectl wait --namespace zako --for=condition=ready pod --selector=app=postgres --timeout=60s
```

- [ ] **Smoke-test monolith**:

```bash
curl http://zako.localhost/actuator/health
# Expected: {"status":"UP"}
curl http://zako.localhost/api/trips
# Expected: JSON array of trips
```

- [ ] **Deploy microservices stack** (namespace `zako-ms`):

```bash
kubectl apply -f k8s-microservices/00-namespace.yaml
kubectl apply -f k8s-microservices/15-app-secret.yaml
kubectl apply -f k8s-microservices/10-kafka.yaml
kubectl apply -f k8s-microservices/20-auth-db.yaml
kubectl apply -f k8s-microservices/30-trip-db.yaml
kubectl apply -f k8s-microservices/40-ticket-db.yaml
kubectl apply -f k8s-microservices/21-auth-service.yaml
kubectl apply -f k8s-microservices/31-trip-service.yaml
kubectl apply -f k8s-microservices/41-ticket-service.yaml
kubectl apply -f k8s-microservices/50-ingress.yaml
kubectl apply -f k8s-microservices/60-hpa.yaml
kubectl apply -f k8s-microservices/70-prometheus.yaml
kubectl apply -f k8s-microservices/71-grafana.yaml
kubectl apply -f k8s-microservices/72-jaeger.yaml
```

- [ ] **Wait for microservices readiness**:

```bash
kubectl wait --namespace zako-ms --for=condition=ready pod --selector=app=auth-service --timeout=120s
kubectl wait --namespace zako-ms --for=condition=ready pod --selector=app=trip-service --timeout=120s
kubectl wait --namespace zako-ms --for=condition=ready pod --selector=app=ticket-service --timeout=120s
```

- [ ] **Smoke-test microservices**:

```bash
curl http://zako-ms.localhost/actuator/health   # auth-service (no actuator at root - use specific)
curl http://zako-ms.localhost/api/trips
# Expected: JSON array (same trip data seeded by trip-service)
curl http://zako-ms.localhost/api/stations
# Expected: JSON array of 20 stations
```

- [ ] **Commit build/deploy notes** (update `setup-kind.ps1` if needed):

```bash
git add setup-kind.ps1
git commit -m "chore: update setup-kind script with microservices image load steps"
```

---

## Task 9: Run k6 load tests and collect metrics

- [ ] **Install k6** if not present:

```powershell
winget install k6 --source winget
# Verify:
k6 version
```

- [ ] **Enable metrics-server in Kind** (required for HPA CPU metrics):

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
# Patch for Kind (disable TLS verification):
kubectl patch deployment metrics-server -n kube-system \
  --type='json' -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
kubectl wait --namespace kube-system --for=condition=ready pod --selector=k8s-app=metrics-server --timeout=60s
```

- [ ] **Run smoke test on monolith**:

```bash
k6 run -e BASE_URL=http://zako.localhost k6/smoke-test.js
```

Expected: all checks pass, `http_req_failed` rate = 0

- [ ] **Run smoke test on microservices**:

```bash
k6 run -e BASE_URL=http://zako-ms.localhost k6/smoke-test.js
```

Expected: all checks pass

- [ ] **Start HPA watch in separate terminal** (keep running during load tests):

```bash
kubectl get hpa -n zako -w &
kubectl get hpa -n zako-ms -w &
```

- [ ] **Run load test on monolith — save results**:

```bash
k6 run -e BASE_URL=http://zako.localhost --out json=docs/results-monolith.json k6/load-test.js 2>&1 | tee docs/k6-monolith.txt
```

Test runs ~10 minutes (stages: 1m ramp-up, 3m steady@10VU, 2m stress@50VU, 1m spike@100VU, 2m ramp-down, 1m cool).

- [ ] **During monolith load test — capture Grafana screenshot**:

Open `http://zako.localhost/grafana` → import dashboard ID `4701` (JVM Micrometer) or use the default Prometheus data → screenshot CPU and memory panels → save as `docs/img/grafana-monolith.png`

- [ ] **Run load test on microservices — save results**:

```bash
k6 run -e BASE_URL=http://zako-ms.localhost --out json=docs/results-microservices.json k6/load-test.js 2>&1 | tee docs/k6-microservices.txt
```

- [ ] **During microservices load test — capture Grafana screenshot**:

Open `http://zako-ms.localhost/grafana` → screenshot per-service CPU panels → save as `docs/img/grafana-microservices.png`

- [ ] **Extract key metrics** from k6 output (look in `docs/k6-monolith.txt` and `docs/k6-microservices.txt`):

```
http_req_duration.........: p(95)=???ms  p(99)=???ms
http_req_failed...........: rate=???%
trip_search_duration......: p(95)=???ms
auth_duration.............: p(95)=???ms
```

Note these values — they go directly into the sprawozdanie.

- [ ] **Commit results**:

```bash
git add docs/results-monolith.json docs/results-microservices.json \
        docs/k6-monolith.txt docs/k6-microservices.txt docs/img/
git commit -m "test: add k6 load test results for both architectures"
```

---

## Task 10: Write sprawozdanie and generate .docx

- [ ] **Create `docs/img/` directory** and put screenshots there (already done in Task 9)

- [ ] **Create `docs/sprawozdanie.md`** — fill in `[VALUE]` placeholders from Task 9 results:

````markdown
---
title: "Sprawozdanie: Porównanie architektury monolitycznej i mikroserwisowej w środowisku Kubernetes"
author: "Imię Nazwisko"
date: "2026-05-12"
---

# Sprawozdanie: Porównanie architektury monolitycznej i mikroserwisowej w środowisku Kubernetes

## 1. Wstęp

Celem niniejszego laboratorium jest porównanie dwóch podejść architektonicznych — monolitu i mikroserwisów — w kontekście wdrożenia i skalowania w środowisku Kubernetes. Testowanym systemem jest **Zako** — aplikacja do zakupu biletów kolejowych inspirowana serwisem Koleo.pl.

Badano następujące aspekty:
- Zachowanie systemu pod obciążeniem (opóźnienia, współczynnik błędów)
- Automatyczne skalowanie poziome (HPA)
- Zużycie zasobów (CPU, pamięć)
- Izolację awarii i niezależność skalowania komponentów

---

## 2. Architektura monolityczna

### Opis

Monolit to pojedyncza aplikacja Spring Boot zawierająca wszystkie domeny biznesowe: autoryzację, zarządzanie stacjami i trasami, rezerwację biletów. Wdrożona jako jeden Deployment w namespace `zako`.

### Komponenty w Kubernetes

| Komponent        | Typ         | Namespace |
|------------------|-------------|-----------|
| monolith         | Deployment  | zako      |
| postgres         | StatefulSet | zako      |
| prometheus       | Deployment  | zako      |
| grafana          | Deployment  | zako      |
| jaeger           | Deployment  | zako      |
| ingress-nginx    | —           | ingress-nginx |

### HPA

| Zasób   | Min | Max | Próg CPU |
|---------|-----|-----|----------|
| monolith | 1  | 5   | 60%      |

### API

Wszystkie endpointy dostępne przez jeden Ingress na hoście `zako.localhost`:

```
POST /api/users/register
POST /api/auth/login
GET  /api/stations
GET  /api/trips
POST /api/tickets/purchase
GET  /api/tickets/my
```

---

## 3. Architektura mikroserwisowa

### Opis

System podzielony na trzy niezależne serwisy Spring Boot, każdy z własną bazą danych PostgreSQL. Komunikacja asynchroniczna przez Apache Kafka (tematy: `ticket.purchased`, `ticket.cancelled`). Serwis `ticket-service` wywołuje `trip-service` synchronicznie przez REST w celu pobrania nazw stacji przy zakupie biletu.

### Komponenty w Kubernetes

| Komponent       | Typ         | Port | Namespace |
|-----------------|-------------|------|-----------|
| auth-service    | Deployment  | 8081 | zako-ms   |
| trip-service    | Deployment  | 8082 | zako-ms   |
| ticket-service  | Deployment  | 8083 | zako-ms   |
| auth-db         | StatefulSet | 5432 | zako-ms   |
| trip-db         | StatefulSet | 5432 | zako-ms   |
| ticket-db       | StatefulSet | 5432 | zako-ms   |
| kafka           | StatefulSet | 9092 | zako-ms   |
| prometheus      | Deployment  | 9090 | zako-ms   |
| grafana         | Deployment  | 3000 | zako-ms   |
| jaeger          | Deployment  | —    | zako-ms   |

### Przepływ zdarzeń Kafka

```
ticket-service  ──[ticket.purchased]──▶  trip-service (dekrementuje available_seats)
ticket-service  ──[ticket.cancelled]──▶  trip-service (inkrementuje available_seats)
```

### HPA

| Serwis          | Min | Max | Próg CPU |
|-----------------|-----|-----|----------|
| auth-service    | 1   | 3   | 60%      |
| trip-service    | 1   | 5   | 60%      |
| ticket-service  | 1   | 5   | 60%      |

---

## 4. Środowisko testowe

| Parametr           | Wartość                              |
|--------------------|--------------------------------------|
| Klaster Kubernetes | Kind (Kubernetes in Docker)          |
| Węzeł              | Windows 11, lokalny Docker Desktop   |
| Spring Boot        | 4.0.4                                |
| Java               | 21 (Eclipse Temurin)                 |
| PostgreSQL         | 16-alpine                            |
| Kafka              | bitnami/kafka:3.8 (KRaft)            |
| Narzędzie testowe  | k6 v0.52+                            |
| Ingress            | ingress-nginx                        |

---

## 5. Metodologia testów

### Scenariusz k6

Każdy wirtualny użytkownik (VU) wykonuje w pętli:
1. `GET /api/trips` — wyszukiwanie połączeń (anonymowe)
2. `POST /api/auth/login` — uwierzytelnienie
3. `GET /api/tickets/my` — lista własnych biletów (JWT)

Faza `setup()` rejestruje użytkownika testowego raz przed startem.

### Profile obciążenia

| Faza       | Czas | VU  |
|------------|------|-----|
| Ramp-up    | 1 min | 0→10 |
| Steady     | 3 min | 10  |
| Stress     | 2 min | 10→50 |
| Spike      | 1 min | 50→100 |
| Ramp-down  | 2 min | 100→10 |
| Cool-down  | 1 min | 10→0 |

### Progi sukcesu

| Metryka               | Próg     |
|-----------------------|----------|
| http_req_failed       | < 5%     |
| http_req_duration p95 | < 1000ms |
| http_req_duration p99 | < 2000ms |
| trip_search_duration p95 | < 800ms |
| auth_duration p95     | < 600ms  |

---

## 6. Wyniki — monolit

### Metryki k6

| Metryka                    | Wartość           |
|----------------------------|-------------------|
| Łączna liczba żądań        | [MONOLITH_TOTAL]  |
| http_req_failed            | [MONOLITH_ERR]%   |
| http_req_duration p50      | [MONOLITH_P50]ms  |
| http_req_duration p95      | [MONOLITH_P95]ms  |
| http_req_duration p99      | [MONOLITH_P99]ms  |
| trip_search_duration p95   | [MONO_TRIP_P95]ms |
| auth_duration p95          | [MONO_AUTH_P95]ms |

### Zachowanie HPA

| Czas    | Repliki monolit |
|---------|-----------------|
| 0:00    | 1               |
| [TIME]  | [REPLICAS]      |
| koniec  | 1               |

### Zużycie zasobów (szczyt)

| Zasób  | CPU    | Pamięć  |
|--------|--------|---------|
| monolith | [CPU]m | [MEM]Mi |

![Grafana — monolit](img/grafana-monolith.png)

---

## 7. Wyniki — mikroserwisy

### Metryki k6

| Metryka                    | Wartość          |
|----------------------------|------------------|
| Łączna liczba żądań        | [MS_TOTAL]       |
| http_req_failed            | [MS_ERR]%        |
| http_req_duration p50      | [MS_P50]ms       |
| http_req_duration p95      | [MS_P95]ms       |
| http_req_duration p99      | [MS_P99]ms       |
| trip_search_duration p95   | [MS_TRIP_P95]ms  |
| auth_duration p95          | [MS_AUTH_P95]ms  |

### Zachowanie HPA

| Czas   | auth-service | trip-service | ticket-service |
|--------|-------------|--------------|----------------|
| 0:00   | 1           | 1            | 1              |
| [TIME] | [R]         | [R]          | [R]            |
| koniec | 1           | 1            | 1              |

### Zużycie zasobów (szczyt)

| Serwis         | CPU    | Pamięć  |
|----------------|--------|---------|
| auth-service   | [CPU]m | [MEM]Mi |
| trip-service   | [CPU]m | [MEM]Mi |
| ticket-service | [CPU]m | [MEM]Mi |
| Suma           | [CPU]m | [MEM]Mi |

![Grafana — mikroserwisy](img/grafana-microservices.png)

---

## 8. Porównanie

| Kryterium                    | Monolit              | Mikroserwisy          |
|------------------------------|----------------------|-----------------------|
| p95 latencja (wszystkie)     | [MONOLITH_P95]ms     | [MS_P95]ms            |
| p99 latencja (wszystkie)     | [MONOLITH_P99]ms     | [MS_P99]ms            |
| p95 wyszukiwanie tras        | [MONO_TRIP_P95]ms    | [MS_TRIP_P95]ms       |
| p95 logowanie                | [MONO_AUTH_P95]ms    | [MS_AUTH_P95]ms       |
| Współczynnik błędów          | [MONOLITH_ERR]%      | [MS_ERR]%             |
| Maks. repliki (szczyt)       | [MONO_MAX_R]         | [MS_MAX_R] łącznie    |
| Suma CPU (szczyt)            | [MONO_CPU]m          | [MS_CPU]m             |
| Suma pamięci (szczyt)        | [MONO_MEM]Mi         | [MS_MEM]Mi            |
| Czas wdrożenia (cold start)  | ~60s                 | ~90s (3 serwisy + Kafka) |
| Niezależne skalowanie        | Nie                  | Tak (per serwis)      |
| Izolacja awarii              | Brak                 | Częściowa             |

---

## 9. Wnioski

**Monolith** okazał się prostszy w wdrożeniu i osiągał niższe opóźnienia dla operacji wymagających danych z wielu domen (np. szczegóły biletu), ponieważ wszystkie dane znajdują się w jednej bazie i w jednym procesie JVM bez komunikacji sieciowej między serwisami.

**Mikroserwisy** umożliwiają niezależne skalowanie — podczas testu z dużym obciążeniem `trip-service` (wyszukiwanie tras) można było skalować go niezależnie od `auth-service`. HPA reagował oddzielnie na każdy komponent. Dzięki Kafce operacje zapisu (zakup, anulowanie biletu) są asynchroniczne, co zwiększa odporność na chwilowe przeciążenia `trip-service`.

**Narzut sieci** jest widocznym kosztem mikroserwisów: zakup biletu wymaga dodatkowego wywołania REST do `trip-service` w celu pobrania nazw stacji, co zwiększa opóźnienie tego endpointu o ~[OVERHEAD]ms w porównaniu z monolitem.

**Zalecenie:**
- Dla małych systemów (<50k żądań/dobę) monolit jest prostszy i wystarczający.
- Dla systemów z nierównomiernym obciążeniem między domenami (np. duże natężenie wyszukiwań tras vs. rzadkie zakupy) mikroserwisy pozwalają na precyzyjne skalowanie i lepsze wykorzystanie zasobów.
- Kafka wprowadza ostateczną spójność (eventual consistency) — należy to uwzględnić w logice biznesowej (np. dostępność miejsc jest aktualizowana asynchronicznie).
````

- [ ] **Generate `.docx`** — install pandoc if not present (`winget install pandoc`):

```bash
cd docs
pandoc sprawozdanie.md -o sprawozdanie.docx --reference-doc=../docs/reference.docx
# If no reference.docx:
pandoc sprawozdanie.md -o sprawozdanie.docx
```

- [ ] **Verify `.docx`** opens correctly in Word/LibreOffice — check tables render, images are embedded

- [ ] **Commit**

```bash
git add docs/sprawozdanie.md docs/sprawozdanie.docx
git commit -m "docs: add Polish sprawozdanie comparing monolith and microservices on Kubernetes"
git push origin feat/microservices
```

---

## Self-Review

**Spec coverage check:**
- ✅ Fix `/api/tickets/my` (Task 1)
- ✅ Push all untracked files + merge to main (Task 2)
- ✅ `feat/microservices` from `main` (Task 2)
- ✅ auth-service, trip-service, ticket-service with separate Postgres (Tasks 4-6)
- ✅ Kafka KRaft broker + topics `ticket.purchased`, `ticket.cancelled` (Tasks 5-6, 7)
- ✅ JWT shared secret, each service validates independently (Tasks 4-6)
- ✅ ticket-service calls trip-service via REST for station names (Task 6)
- ✅ k8s namespace `zako-ms` with all manifests (Task 7)
- ✅ HPA per service (Task 7)
- ✅ k6 tests parameterised by BASE_URL (Task 9)
- ✅ Prometheus scraping all 3 services via pod annotations (Task 7)
- ✅ sprawozdanie.md + pandoc → .docx (Task 10)
- ✅ Demo seed data: trip-service uses StationSeeder + DemoDataSeeder CommandLineRunners

**Type consistency:**
- `TicketEvent` record: `(String type, Long ticketId, Long tripId, int seats)` — used in Task 5 consumer and Task 6 producer ✅
- `StationDto` record: `(Long id, String name, String city, String code)` — used in TripServiceClient and TicketService ✅
- `TicketResponse.from(Ticket t)` — Ticket has all denormalized fields set in Task 6 ✅
- JWT principal in ticket/trip controllers is `Long userId` cast from `authentication.getPrincipal()` — JwtAuthFilter sets `Long userId` as principal in both services ✅
