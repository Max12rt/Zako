# Zako Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add JWT auth, payment simulation, booking flow UI, my-tickets page, Flyway migrations, structured JSON logging, and Kubernetes-ready health probes to the Zako train-ticket platform.

**Architecture:** Spring Boot 4 monolith + Angular 21 standalone SPA. Backend gets JWT (jjwt 0.12.6) stateless auth, Flyway schema migrations, Spring Actuator health endpoint, and logstash JSON logging. Frontend gets Angular routing, auth service with signals + localStorage, JWT interceptor, and four new pages (login, register, book, my-tickets).

**Tech Stack:** Java 21, Spring Boot 4.0.4, Spring Security 6, jjwt 0.12.6, Flyway 10, logstash-logback-encoder 8, Angular 21, standalone components, signals, Angular Router.

---

## File Map

**Backend — new files:**
- `monolith/src/main/java/zako/monolith/config/JwtService.java`
- `monolith/src/main/java/zako/monolith/config/JwtAuthFilter.java`
- `monolith/src/main/java/zako/monolith/auth/AuthController.java`
- `monolith/src/main/java/zako/monolith/auth/AuthService.java`
- `monolith/src/main/java/zako/monolith/auth/dto/LoginResponse.java`
- `monolith/src/main/java/zako/monolith/ticket/PaymentStatus.java`
- `monolith/src/main/resources/db/migration/V1__init.sql`
- `monolith/src/main/resources/db/migration/V2__add_payment_status.sql`
- `monolith/src/main/resources/logback-spring.xml`

**Backend — modified files:**
- `monolith/pom.xml`
- `monolith/src/main/java/zako/monolith/config/SecurityConfig.java`
- `monolith/src/main/java/zako/monolith/ticket/Ticket.java`
- `monolith/src/main/java/zako/monolith/ticket/TicketService.java`
- `monolith/src/main/java/zako/monolith/ticket/TicketController.java`
- `monolith/src/main/resources/application.properties`

**Frontend — new files:**
- `frontend/src/app/home/home.ts`
- `frontend/src/app/home/home.html`
- `frontend/src/app/home/home.scss`
- `frontend/src/app/auth/auth.service.ts`
- `frontend/src/app/auth/auth-api.service.ts`
- `frontend/src/app/auth/auth.interceptor.ts`
- `frontend/src/app/auth/auth.guard.ts`
- `frontend/src/app/pages/login/login.ts`
- `frontend/src/app/pages/login/login.html`
- `frontend/src/app/pages/login/login.scss`
- `frontend/src/app/pages/register/register.ts`
- `frontend/src/app/pages/register/register.html`
- `frontend/src/app/pages/register/register.scss`
- `frontend/src/app/pages/book/book.ts`
- `frontend/src/app/pages/book/book.html`
- `frontend/src/app/pages/book/book.scss`
- `frontend/src/app/pages/my-tickets/my-tickets.ts`
- `frontend/src/app/pages/my-tickets/my-tickets.html`
- `frontend/src/app/pages/my-tickets/my-tickets.scss`
- `frontend/src/app/services/ticket-api.service.ts`

**Frontend — modified files:**
- `frontend/src/app/app.ts`
- `frontend/src/app/app.html`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/app.config.ts`
- `frontend/src/app/header-menu/header-menu.ts`
- `frontend/src/app/header-menu/header-menu.html`

**K8s — modified files:**
- `k8s/21-monolith.yaml`

---

## Task 1: Add Maven dependencies

**Files:**
- Modify: `monolith/pom.xml`

- [ ] **Step 1: Add jjwt, Actuator, Flyway, logstash-logback-encoder inside `<dependencies>`**

Replace the existing `<dependencies>` block in `monolith/pom.xml` with:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Flyway -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- JSON logging -->
    <dependency>
        <groupId>net.logstash.logback</groupId>
        <artifactId>logstash-logback-encoder</artifactId>
        <version>8.0</version>
    </dependency>

    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

- [ ] **Step 2: Verify compilation**

```
cd monolith && mvnw.cmd compile -q
```
Expected: BUILD SUCCESS (dependencies download, no compile errors).

- [ ] **Step 3: Commit**

```bash
git add monolith/pom.xml
git commit -m "build(monolith): add jjwt, actuator, flyway, logstash dependencies"
```

---

## Task 2: Flyway migrations + application.properties

**Files:**
- Create: `monolith/src/main/resources/db/migration/V1__init.sql`
- Create: `monolith/src/main/resources/db/migration/V2__add_payment_status.sql`
- Modify: `monolith/src/main/resources/application.properties`

- [ ] **Step 1: Create V1__init.sql** (full schema, `IF NOT EXISTS` so it's idempotent on existing DBs)

```sql
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    first_name  VARCHAR(255) NOT NULL,
    last_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS stations (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(255) UNIQUE NOT NULL,
    city      VARCHAR(255) NOT NULL,
    code      VARCHAR(255) UNIQUE NOT NULL,
    latitude  DOUBLE PRECISION,
    longitude DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS trains (
    id           BIGSERIAL PRIMARY KEY,
    train_number VARCHAR(255) UNIQUE NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    total_seats  INTEGER      NOT NULL
);

CREATE TABLE IF NOT EXISTS routes (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS route_stops (
    id         BIGSERIAL PRIMARY KEY,
    route_id   BIGINT  NOT NULL REFERENCES routes(id),
    station_id BIGINT  NOT NULL REFERENCES stations(id),
    stop_order INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS trips (
    id               BIGSERIAL PRIMARY KEY,
    train_id         BIGINT      NOT NULL REFERENCES trains(id),
    route_id         BIGINT      NOT NULL REFERENCES routes(id),
    departure_time   TIMESTAMP   NOT NULL,
    arrival_time     TIMESTAMP   NOT NULL,
    status           VARCHAR(50) NOT NULL,
    available_seats  INTEGER     NOT NULL
);

CREATE TABLE IF NOT EXISTS tickets (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT         NOT NULL REFERENCES users(id),
    trip_id         BIGINT         NOT NULL REFERENCES trips(id),
    from_station_id BIGINT         NOT NULL REFERENCES stations(id),
    to_station_id   BIGINT         NOT NULL REFERENCES stations(id),
    seat_number     INTEGER,
    price           NUMERIC(10, 2) NOT NULL,
    status          VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    ticket_code     VARCHAR(255)   UNIQUE NOT NULL,
    purchased_at    TIMESTAMP      NOT NULL DEFAULT now()
);
```

- [ ] **Step 2: Create V2__add_payment_status.sql**

```sql
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING';
```

- [ ] **Step 3: Update application.properties**

Replace the entire file with:

```properties
spring.application.name=monolith

server.port=${SERVER_PORT:9090}
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s

# Datasource
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/ticket_monolith_db}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:1234}
spring.datasource.driver-class-name=org.postgresql.Driver

# Hibernate — schema managed by Flyway
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=${SPRING_JPA_SHOW_SQL:false}
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# HikariCP
spring.datasource.hikari.maximum-pool-size=${HIKARI_MAX_POOL:5}
spring.datasource.hikari.minimum-idle=${HIKARI_MIN_IDLE:1}
spring.datasource.hikari.connection-timeout=${HIKARI_CONN_TIMEOUT_MS:20000}

# Flyway — baseline-on-migrate=true for existing local DBs; set false for fresh K8s deploys
spring.flyway.baseline-on-migrate=${FLYWAY_BASELINE_ON_MIGRATE:true}
spring.flyway.baseline-version=1
spring.flyway.validate-on-migrate=false

# JWT
jwt.secret=${JWT_SECRET:ZGV2LXNlY3JldC1rZXktZm9yLXpha28tYXBwLTI1NmJpdHM=}
jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}

# Actuator
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
```

- [ ] **Step 4: Commit**

```bash
git add monolith/src/main/resources/db/migration/ monolith/src/main/resources/application.properties
git commit -m "feat(monolith): add flyway migrations and update application config"
```

---

## Task 3: JWT backend — JwtService + JwtAuthFilter + SecurityConfig

**Files:**
- Create: `monolith/src/main/java/zako/monolith/config/JwtService.java`
- Create: `monolith/src/main/java/zako/monolith/config/JwtAuthFilter.java`
- Modify: `monolith/src/main/java/zako/monolith/config/SecurityConfig.java`

- [ ] **Step 1: Create JwtService.java**

```java
package zako.monolith.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import zako.monolith.user.User;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key())
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
```

- [ ] **Step 2: Create JwtAuthFilter.java**

```java
package zako.monolith.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import zako.monolith.user.User;
import zako.monolith.user.UserRepository;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        if (!jwtService.isValid(token)) {
            chain.doFilter(request, response);
            return;
        }
        String email = jwtService.extractEmail(token);
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            userRepository.findByEmail(email).ifPresent(user -> {
                var auth = new UsernamePasswordAuthenticationToken(
                        user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 3: Replace SecurityConfig.java**

```java
package zako.monolith.config;

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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trips/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/trips/search").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 4: Compile to verify no errors**

```
cd monolith && mvnw.cmd compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add monolith/src/main/java/zako/monolith/config/
git commit -m "feat(auth): add JWT service, filter, and secure endpoints"
```

---

## Task 4: Auth endpoints — login + me

**Files:**
- Create: `monolith/src/main/java/zako/monolith/auth/dto/LoginResponse.java`
- Create: `monolith/src/main/java/zako/monolith/auth/AuthService.java`
- Create: `monolith/src/main/java/zako/monolith/auth/AuthController.java`

- [ ] **Step 1: Create LoginResponse.java**

```java
package zako.monolith.auth.dto;

import zako.monolith.user.dto.UserResponse;

public record LoginResponse(String token, UserResponse user) {}
```

- [ ] **Step 2: Create AuthService.java**

```java
package zako.monolith.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import zako.monolith.auth.dto.LoginResponse;
import zako.monolith.config.JwtService;
import zako.monolith.user.UserRepository;
import zako.monolith.user.dto.LoginRequest;
import zako.monolith.user.dto.UserResponse;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return new LoginResponse(jwtService.generateToken(user), UserResponse.from(user));
    }
}
```

- [ ] **Step 3: Create AuthController.java**

```java
package zako.monolith.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import zako.monolith.auth.dto.LoginResponse;
import zako.monolith.user.User;
import zako.monolith.user.dto.LoginRequest;
import zako.monolith.user.dto.UserResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
```

- [ ] **Step 4: Compile**

```
cd monolith && mvnw.cmd compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add monolith/src/main/java/zako/monolith/auth/
git commit -m "feat(auth): add /api/auth/login and /api/auth/me endpoints"
```

---

## Task 5: Payment simulation

**Files:**
- Create: `monolith/src/main/java/zako/monolith/ticket/PaymentStatus.java`
- Modify: `monolith/src/main/java/zako/monolith/ticket/Ticket.java`
- Modify: `monolith/src/main/java/zako/monolith/ticket/TicketService.java`
- Modify: `monolith/src/main/java/zako/monolith/ticket/TicketController.java`
- Modify: `monolith/src/main/java/zako/monolith/ticket/dto/TicketResponse.java`

- [ ] **Step 1: Create PaymentStatus.java**

```java
package zako.monolith.ticket;

public enum PaymentStatus {
    PENDING, PAID
}
```

- [ ] **Step 2: Add paymentStatus to Ticket.java**

Add field after the `status` field in `Ticket.java`:

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
@Builder.Default
private PaymentStatus paymentStatus = PaymentStatus.PENDING;
```

Also add import: `import zako.monolith.ticket.PaymentStatus;`

- [ ] **Step 3: Add paymentStatus to TicketResponse.java**

Add `PaymentStatus paymentStatus` to the record fields and update the `from()` method:

```java
package zako.monolith.ticket.dto;

import zako.monolith.station.dto.StationResponse;
import zako.monolith.ticket.Ticket;
import zako.monolith.ticket.PaymentStatus;
import zako.monolith.ticket.TicketStatus;
import zako.monolith.trip.dto.TripResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        Long userId,
        TripResponse trip,
        StationResponse fromStation,
        StationResponse toStation,
        Integer seatNumber,
        BigDecimal price,
        TicketStatus status,
        PaymentStatus paymentStatus,
        String ticketCode,
        LocalDateTime purchasedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getUser().getId(),
                TripResponse.from(ticket.getTrip()),
                StationResponse.from(ticket.getFromStation()),
                StationResponse.from(ticket.getToStation()),
                ticket.getSeatNumber(),
                ticket.getPrice(),
                ticket.getStatus(),
                ticket.getPaymentStatus(),
                ticket.getTicketCode(),
                ticket.getPurchasedAt()
        );
    }
}
```

- [ ] **Step 4: Add pay() method to TicketService.java**

Add import `import zako.monolith.ticket.PaymentStatus;` and add this method:

```java
@Transactional
public TicketResponse pay(Long ticketId, Long userId) {
    Ticket ticket = findById(ticketId);
    if (!ticket.getUser().getId().equals(userId)) {
        throw new IllegalStateException("Not authorized to pay for this ticket");
    }
    if (ticket.getPaymentStatus() == PaymentStatus.PAID) {
        throw new IllegalStateException("Ticket already paid");
    }
    ticket.setPaymentStatus(PaymentStatus.PAID);
    return TicketResponse.from(ticketRepository.save(ticket));
}
```

- [ ] **Step 5: Replace TicketController.java** (use Authentication, add /pay, remove @RequestParam userId)

```java
package zako.monolith.ticket;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import zako.monolith.ticket.dto.TicketRequest;
import zako.monolith.ticket.dto.TicketResponse;
import zako.monolith.user.User;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse purchase(@Valid @RequestBody TicketRequest request,
                                   Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ticketService.purchase(user.getId(), request);
    }

    @PostMapping("/{id}/pay")
    public TicketResponse pay(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ticketService.pay(id, user.getId());
    }

    @PostMapping("/{id}/cancel")
    public TicketResponse cancel(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ticketService.cancel(id, user.getId());
    }

    @GetMapping("/user/{userId}")
    public List<TicketResponse> getUserTickets(@PathVariable Long userId) {
        return ticketService.getUserTickets(userId);
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable Long id) {
        return ticketService.getById(id);
    }
}
```

- [ ] **Step 6: Compile**

```
cd monolith && mvnw.cmd compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add monolith/src/main/java/zako/monolith/ticket/
git commit -m "feat(tickets): add payment simulation (PENDING→PAID) and use JWT for auth"
```

---

## Task 6: Logback JSON + K8s health probes

**Files:**
- Create: `monolith/src/main/resources/logback-spring.xml`
- Modify: `k8s/21-monolith.yaml`

- [ ] **Step 1: Create logback-spring.xml**

```xml
<configuration>

  <springProfile name="default,!prod">
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
      <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
      </encoder>
    </appender>
    <root level="INFO">
      <appender-ref ref="CONSOLE"/>
    </root>
  </springProfile>

  <springProfile name="prod">
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
      <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    <root level="INFO">
      <appender-ref ref="JSON"/>
    </root>
  </springProfile>

</configuration>
```

- [ ] **Step 2: Update K8s probes in k8s/21-monolith.yaml** — change both `tcpSocket` probes to `httpGet /actuator/health`

Replace the `readinessProbe` and `livenessProbe` sections with:

```yaml
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 9090
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 9090
            initialDelaySeconds: 60
            periodSeconds: 30
```

Also add `SPRING_PROFILES_ACTIVE: prod` to the ConfigMap reference in `k8s/20-monolith-config.yaml`:

- [ ] **Step 3: Read k8s/20-monolith-config.yaml and add SPRING_PROFILES_ACTIVE**

Open `k8s/20-monolith-config.yaml` and add `SPRING_PROFILES_ACTIVE: "prod"` to the ConfigMap data section.

- [ ] **Step 4: Commit**

```bash
git add monolith/src/main/resources/logback-spring.xml k8s/21-monolith.yaml k8s/20-monolith-config.yaml
git commit -m "feat(ops): add JSON logging profile and upgrade k8s probes to HTTP health endpoint"
```

---

## Task 7: Frontend routing restructure

Move current App search/home content into a HomeComponent so App can become the pure shell with `<router-outlet />`.

**Files:**
- Create: `frontend/src/app/home/home.ts`
- Create: `frontend/src/app/home/home.html`
- Create: `frontend/src/app/home/home.scss`
- Modify: `frontend/src/app/app.ts`
- Modify: `frontend/src/app/app.html`

- [ ] **Step 1: Create home/home.ts** (move all logic from app.ts)

```typescript
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HeaderMenu } from '../header-menu/header-menu';
import { DatePicker } from '../date-picker/date-picker';
import { StationInput } from '../station-input/station-input';
import { StationDto } from '../services/station.service';
import { TripResponseDto, TripService } from '../services/trip.service';

@Component({
  selector: 'app-home',
  imports: [CommonModule, HeaderMenu, DatePicker, StationInput],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class HomeComponent {
  private trips = inject(TripService);
  private router = inject(Router);

  searchDate = signal<Date>(new Date());
  fromStation = signal<StationDto | null>(null);
  toStation = signal<StationDto | null>(null);

  results = signal<TripResponseDto[]>([]);
  searching = signal(false);
  searched = signal(false);
  searchError = signal<string | null>(null);

  swapStations() {
    const a = this.fromStation();
    const b = this.toStation();
    this.fromStation.set(b);
    this.toStation.set(a);
  }

  goBook(tripId: number) {
    const from = this.fromStation();
    const to = this.toStation();
    this.router.navigate(['/book', tripId], {
      queryParams: { from: from?.id, to: to?.id }
    });
  }

  submit() {
    this.searchError.set(null);
    const from = this.fromStation();
    const to = this.toStation();
    if (!from || !to) {
      this.searchError.set('Wybierz stację odjazdu i przyjazdu z listy.');
      return;
    }
    if (from.id === to.id) {
      this.searchError.set('Stacja odjazdu i przyjazdu muszą być różne.');
      return;
    }
    this.searching.set(true);
    this.searched.set(true);
    const date = this.searchDate().toISOString().slice(0, 10);
    this.trips.search({ fromStationId: from.id, toStationId: to.id, date }).subscribe({
      next: list => { this.results.set(list); this.searching.set(false); },
      error: () => { this.searchError.set('Błąd wyszukiwania połączeń.'); this.searching.set(false); }
    });
  }
}
```

- [ ] **Step 2: Create home/home.html** (copy app.html content, remove `<router-outlet />`, add Kup button to results)

```html
<main class="page">
  <section class="hero">
    <div class="hero__inner">
      <header class="hero__topbar">
        <div class="logo-group">
          <a class="logo" href="/" aria-label="KOLEO – strona główna">
            <svg class="logo__mark" viewBox="0 0 28 22" aria-hidden="true">
              <path d="M5 3 L20 11 L5 19" fill="none" stroke="#3ed3c4" stroke-width="5.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span class="logo__word">KOLEO</span>
          </a>
          <p class="logo__tagline">ROZKŁAD JAZDY PKP I BILETY</p>
        </div>
        <app-header-menu />
      </header>

      <h1 class="hero__title">Dokąd jedziemy?</h1>

      <div class="search">
        <div class="search-bar">
          <app-station-input
            placeholder="Stacja odjazdu"
            leadingIcon="depart"
            [showLocateButton]="true"
            [value]="fromStation()"
            (stationSelected)="fromStation.set($event)" />

          <app-station-input
            placeholder="Stacja przyjazdu"
            leadingIcon="arrive"
            trailingIcon="none"
            [value]="toStation()"
            (stationSelected)="toStation.set($event)">
            <button trailing type="button" class="swap-btn"
                    (click)="swapStations(); $event.stopPropagation()"
                    [disabled]="!fromStation() && !toStation()">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M7 8h10m-3-3l3 3-3 3M17 16H7m3 3l-3-3 3-3" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span>Zamień stacje</span>
            </button>
          </app-station-input>

          <app-date-picker [value]="searchDate()" (valueChange)="searchDate.set($event)" />
        </div>

        <button type="button" class="search__cta" [disabled]="searching()" (click)="submit()">
          <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
            <circle cx="11" cy="11" r="6" fill="none" stroke="currentColor" stroke-width="2"/>
            <path d="M16 16l4 4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <span>{{ searching() ? 'Szukam…' : 'Znajdź połączenie' }}</span>
        </button>
      </div>

      @if (searchError()) {
        <p class="search__error">{{ searchError() }}</p>
      }

      @if (results().length > 0) {
        <ul class="results" role="list">
          @for (t of results(); track t.id) {
            <li class="results__item">
              <span class="results__time">{{ t.departureTime | slice:11:16 }} – {{ t.arrivalTime | slice:11:16 }}</span>
              <span class="results__route">{{ t.routeName }}</span>
              <span class="results__train">{{ t.train.type }} {{ t.train.number }}</span>
              <span class="results__seats">{{ t.availableSeats }} miejsc</span>
              <button type="button" class="results__buy" (click)="goBook(t.id)">Kup</button>
            </li>
          }
        </ul>
      } @else if (searched() && !searching() && !searchError()) {
        <p class="search__empty">Brak połączeń dla wybranych kryteriów.</p>
      }
    </div>
  </section>

  <section class="operators">
    <div class="operators__inner">
      <h2 class="operators__title">Wszystkie bilety w jednym miejscu</h2>
      <ul class="operators__grid" role="list">
        <li class="op op--pkp">PKP <span>INTERCITY</span></li>
        <li class="op op--polregio">PolREGIO</li>
        <li class="op op--flixbus">FLIXBUS</li>
        <li class="op op--arriva">arriva</li>
        <li class="op op--kw">Koleje<br/>Wielkopolskie</li>
        <li class="op op--kd">Koleje Dolnośląskie</li>
        <li class="op op--ksl">Koleje Śląskie</li>
        <li class="op op--km">Koleje<br/>Mazowieckie</li>
        <li class="op op--kmal">Koleje<br/>Małopolskie</li>
        <li class="op op--skm">SKM</li>
        <li class="op op--lka">ŁKA</li>
        <li class="op op--leo">Leo Express</li>
        <li class="op op--regiojet"><span class="rj-bars">||</span>REGIOJET</li>
      </ul>
    </div>
  </section>
</main>
```

- [ ] **Step 3: Create home/home.scss** (copy content from app.scss)

```
cp frontend/src/app/app.scss frontend/src/app/home/home.scss
```

- [ ] **Step 4: Replace app.ts** (slim shell — just router-outlet)

```typescript
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />'
})
export class App {}
```

- [ ] **Step 5: Replace app.html** with just the router-outlet (the template is now inline in app.ts — delete app.html content or leave it empty; since template is inline, app.html is no longer referenced)**

Since the template is now inline in app.ts, delete the content of app.html (or leave it unused). The `templateUrl` is removed from app.ts.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/home/ frontend/src/app/app.ts frontend/src/app/app.html
git commit -m "refactor(frontend): extract HomeComponent, make App a router shell"
```

---

## Task 8: Angular auth infrastructure

**Files:**
- Create: `frontend/src/app/auth/auth-api.service.ts`
- Create: `frontend/src/app/auth/auth.service.ts`
- Create: `frontend/src/app/auth/auth.interceptor.ts`
- Create: `frontend/src/app/auth/auth.guard.ts`
- Modify: `frontend/src/app/app.config.ts`

- [ ] **Step 1: Create auth-api.service.ts**

```typescript
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface LoginRequest { email: string; password: string; }
export interface RegisterRequest { firstName: string; lastName: string; email: string; password: string; }
export interface UserDto { id: number; firstName: string; lastName: string; email: string; role: string; }
export interface LoginResponse { token: string; user: UserDto; }

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private http = inject(HttpClient);

  login(req: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', req);
  }

  register(req: RegisterRequest): Observable<UserDto> {
    return this.http.post<UserDto>('/api/users/register', req);
  }

  me(): Observable<UserDto> {
    return this.http.get<UserDto>('/api/auth/me');
  }
}
```

- [ ] **Step 2: Create auth.service.ts**

```typescript
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthApiService, LoginRequest, LoginResponse, RegisterRequest, UserDto } from './auth-api.service';
import { Observable, tap } from 'rxjs';

const TOKEN_KEY = 'zako_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = inject(AuthApiService);
  private router = inject(Router);

  token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  currentUser = signal<UserDto | null>(null);
  isLoggedIn = computed(() => !!this.token());

  login(req: LoginRequest): Observable<LoginResponse> {
    return this.api.login(req).pipe(
      tap(res => {
        localStorage.setItem(TOKEN_KEY, res.token);
        this.token.set(res.token);
        this.currentUser.set(res.user);
      })
    );
  }

  register(req: RegisterRequest): Observable<UserDto> {
    return this.api.register(req);
  }

  logout() {
    localStorage.removeItem(TOKEN_KEY);
    this.token.set(null);
    this.currentUser.set(null);
    this.router.navigate(['/']);
  }

  loadCurrentUser() {
    if (this.token()) {
      this.api.me().subscribe({
        next: user => this.currentUser.set(user),
        error: () => this.logout()
      });
    }
  }
}
```

- [ ] **Step 3: Create auth.interceptor.ts**

```typescript
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).token();
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};
```

- [ ] **Step 4: Create auth.guard.ts**

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.isLoggedIn()) return true;
  inject(Router).navigate(['/login']);
  return false;
};
```

- [ ] **Step 5: Update app.config.ts**

```typescript
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { APP_INITIALIZER } from '@angular/core';
import { routes } from './app.routes';
import { jwtInterceptor } from './auth/auth.interceptor';
import { AuthService } from './auth/auth.service';

function initAuth(auth: AuthService) {
  return () => auth.loadCurrentUser();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withFetch(), withInterceptors([jwtInterceptor])),
    {
      provide: APP_INITIALIZER,
      useFactory: initAuth,
      deps: [AuthService],
      multi: true
    }
  ]
};
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/auth/ frontend/src/app/app.config.ts
git commit -m "feat(frontend): add JWT auth service, interceptor, and guard"
```

---

## Task 9: Login page

**Files:**
- Create: `frontend/src/app/pages/login/login.ts`
- Create: `frontend/src/app/pages/login/login.html`
- Create: `frontend/src/app/pages/login/login.scss`

- [ ] **Step 1: Create login.ts**

```typescript
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class LoginPage {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = signal('');
  password = signal('');
  error = signal<string | null>(null);
  loading = signal(false);

  submit() {
    this.error.set(null);
    this.loading.set(true);
    this.auth.login({ email: this.email(), password: this.password() }).subscribe({
      next: () => this.router.navigate(['/']),
      error: () => { this.error.set('Nieprawidłowy e-mail lub hasło.'); this.loading.set(false); }
    });
  }
}
```

- [ ] **Step 2: Create login.html**

```html
<div class="auth-page">
  <div class="auth-card">
    <a class="auth-logo" href="/">
      <svg viewBox="0 0 28 22" width="28" aria-hidden="true">
        <path d="M5 3 L20 11 L5 19" fill="none" stroke="#3ed3c4" stroke-width="5.5" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <span>KOLEO</span>
    </a>

    <h1 class="auth-card__title">Zaloguj się</h1>

    <div class="auth-form">
      <label class="field">
        <span class="field__label">E-mail</span>
        <input class="field__input" type="email" autocomplete="email"
               [ngModel]="email()" (ngModelChange)="email.set($event)" />
      </label>

      <label class="field">
        <span class="field__label">Hasło</span>
        <input class="field__input" type="password" autocomplete="current-password"
               [ngModel]="password()" (ngModelChange)="password.set($event)" />
      </label>

      @if (error()) {
        <p class="auth-form__error">{{ error() }}</p>
      }

      <button type="button" class="auth-form__btn" [disabled]="loading()" (click)="submit()">
        {{ loading() ? 'Logowanie…' : 'Zaloguj się' }}
      </button>
    </div>

    <p class="auth-card__switch">
      Nie masz konta? <a routerLink="/register">Załóż konto</a>
    </p>
  </div>
</div>
```

- [ ] **Step 3: Create login.scss**

```scss
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0d1b2a;
}

.auth-card {
  background: #162032;
  border: 1px solid rgba(255,255,255,.08);
  border-radius: 16px;
  padding: 40px 36px;
  width: 100%;
  max-width: 400px;
  display: flex;
  flex-direction: column;
  gap: 24px;

  &__title {
    color: #fff;
    font-size: 1.5rem;
    font-weight: 700;
    margin: 0;
  }

  &__switch {
    color: #8899aa;
    font-size: .9rem;
    text-align: center;
    margin: 0;
    a { color: #3ed3c4; text-decoration: none; &:hover { text-decoration: underline; } }
  }
}

.auth-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: #fff;
  font-weight: 800;
  font-size: 1.1rem;
  letter-spacing: .05em;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__btn {
    width: 100%;
    padding: 13px;
    background: #3ed3c4;
    color: #0d1b2a;
    font-weight: 700;
    font-size: 1rem;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    margin-top: 4px;
    &:disabled { opacity: .5; cursor: not-allowed; }
  }

  &__error {
    color: #ff6b6b;
    font-size: .88rem;
    margin: 0;
  }
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;

  &__label {
    color: #8899aa;
    font-size: .85rem;
  }

  &__input {
    background: #0d1b2a;
    border: 1px solid rgba(255,255,255,.15);
    border-radius: 8px;
    padding: 10px 14px;
    color: #fff;
    font-size: 1rem;
    outline: none;
    &:focus { border-color: #3ed3c4; }
  }
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/pages/login/
git commit -m "feat(frontend): add login page"
```

---

## Task 10: Register page

**Files:**
- Create: `frontend/src/app/pages/register/register.ts`
- Create: `frontend/src/app/pages/register/register.html`
- Create: `frontend/src/app/pages/register/register.scss`

- [ ] **Step 1: Create register.ts**

```typescript
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-register',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class RegisterPage {
  private auth = inject(AuthService);
  private router = inject(Router);

  firstName = signal('');
  lastName = signal('');
  email = signal('');
  password = signal('');
  error = signal<string | null>(null);
  loading = signal(false);

  submit() {
    this.error.set(null);
    this.loading.set(true);
    this.auth.register({
      firstName: this.firstName(),
      lastName: this.lastName(),
      email: this.email(),
      password: this.password()
    }).subscribe({
      next: () => {
        this.auth.login({ email: this.email(), password: this.password() }).subscribe({
          next: () => this.router.navigate(['/']),
          error: () => this.router.navigate(['/login'])
        });
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Błąd rejestracji. Sprawdź dane.');
        this.loading.set(false);
      }
    });
  }
}
```

- [ ] **Step 2: Create register.html**

```html
<div class="auth-page">
  <div class="auth-card">
    <a class="auth-logo" href="/">
      <svg viewBox="0 0 28 22" width="28" aria-hidden="true">
        <path d="M5 3 L20 11 L5 19" fill="none" stroke="#3ed3c4" stroke-width="5.5" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <span>KOLEO</span>
    </a>

    <h1 class="auth-card__title">Załóż konto</h1>

    <div class="auth-form">
      <div class="auth-form__row">
        <label class="field">
          <span class="field__label">Imię</span>
          <input class="field__input" type="text" autocomplete="given-name"
                 [ngModel]="firstName()" (ngModelChange)="firstName.set($event)" />
        </label>
        <label class="field">
          <span class="field__label">Nazwisko</span>
          <input class="field__input" type="text" autocomplete="family-name"
                 [ngModel]="lastName()" (ngModelChange)="lastName.set($event)" />
        </label>
      </div>

      <label class="field">
        <span class="field__label">E-mail</span>
        <input class="field__input" type="email" autocomplete="email"
               [ngModel]="email()" (ngModelChange)="email.set($event)" />
      </label>

      <label class="field">
        <span class="field__label">Hasło (min. 8 znaków)</span>
        <input class="field__input" type="password" autocomplete="new-password"
               [ngModel]="password()" (ngModelChange)="password.set($event)" />
      </label>

      @if (error()) {
        <p class="auth-form__error">{{ error() }}</p>
      }

      <button type="button" class="auth-form__btn" [disabled]="loading()" (click)="submit()">
        {{ loading() ? 'Rejestracja…' : 'Załóż konto' }}
      </button>
    </div>

    <p class="auth-card__switch">
      Masz już konto? <a routerLink="/login">Zaloguj się</a>
    </p>
  </div>
</div>
```

- [ ] **Step 3: Create register.scss** (identical to login.scss — copy it)

Copy the content of `login.scss` verbatim into `register.scss`, then add:

```scss
.auth-form__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/pages/register/
git commit -m "feat(frontend): add register page"
```

---

## Task 11: Header-menu auth-aware update

**Files:**
- Modify: `frontend/src/app/header-menu/header-menu.ts`
- Modify: `frontend/src/app/header-menu/header-menu.html`

- [ ] **Step 1: Replace header-menu.ts**

```typescript
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-header-menu',
  imports: [CommonModule, RouterLink],
  templateUrl: './header-menu.html',
  styleUrl: './header-menu.scss'
})
export class HeaderMenu {
  auth = inject(AuthService);
  private router = inject(Router);
  open = signal(false);

  toggle() { this.open.update(v => !v); }
  close() { this.open.set(false); }

  logout() {
    this.auth.logout();
    this.close();
  }

  goLogin() { this.router.navigate(['/login']); this.close(); }
  goRegister() { this.router.navigate(['/register']); this.close(); }
  goMyTickets() { this.router.navigate(['/my-tickets']); this.close(); }
}
```

- [ ] **Step 2: Replace header-menu.html**

```html
<div class="actions-pill" [class.actions-pill--active]="open()">
  <button type="button" class="pill-btn pill-btn--login" (click)="toggle()">
    @if (auth.isLoggedIn()) {
      <span>{{ auth.currentUser()?.firstName ?? 'Konto' }}</span>
    } @else {
      <span>Zaloguj się</span>
    }
    <svg class="pill-btn__avatar" viewBox="0 0 28 28" width="28" height="28" aria-hidden="true">
      <circle cx="14" cy="14" r="13" fill="none" stroke="currentColor" stroke-width="1.5"/>
      <circle cx="14" cy="11" r="3.4" fill="none" stroke="currentColor" stroke-width="1.5"/>
      <path d="M6.5 22.5c1.6-3.2 4.4-4.8 7.5-4.8s5.9 1.6 7.5 4.8" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
    </svg>
  </button>

  <button type="button" class="pill-btn pill-btn--menu" [attr.aria-label]="open() ? 'Zamknij menu' : 'Otwórz menu'" (click)="toggle()">
    @if (open()) {
      <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
        <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
    } @else {
      <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
        <path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
    }
  </button>
</div>

@if (open()) {
  <div class="panel" role="menu">
    @if (!auth.isLoggedIn()) {
      <button type="button" class="panel__btn panel__btn--ghost" (click)="goLogin()">Zaloguj się</button>
      <div class="panel__divider"><span>lub</span></div>
      <button type="button" class="panel__btn panel__btn--primary" (click)="goRegister()">Załóż konto</button>
    } @else {
      <div class="panel__user">
        <span class="panel__user-name">{{ auth.currentUser()?.firstName }} {{ auth.currentUser()?.lastName }}</span>
        <span class="panel__user-email">{{ auth.currentUser()?.email }}</span>
      </div>
    }

    <ul class="panel__items" role="list">
      <li class="panel__item" (click)="goMyTickets()">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="3" y="6" width="18" height="12" rx="2" fill="none" stroke="currentColor" stroke-width="1.8"/>
          <path d="M3 11h18" stroke="currentColor" stroke-width="1.8"/>
        </svg>
        <span>Moje bilety</span>
        <svg class="chev" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
      </li>
    </ul>

    @if (auth.isLoggedIn()) {
      <div class="panel__sep"></div>
      <button type="button" class="panel__btn panel__btn--ghost" (click)="logout()">Wyloguj się</button>
    }

    <div class="panel__sep"></div>
    <button type="button" class="panel__lang">
      <span class="flag" aria-hidden="true">
        <span class="flag__top"></span>
        <span class="flag__bot"></span>
      </span>
      <span class="panel__lang-label">PL</span>
      <svg class="chev chev--teal" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
    </button>
  </div>
}
```

Also add `.panel__user` styles to `header-menu.scss`:

```scss
.panel__user {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.panel__user-name {
  font-weight: 600;
  color: #fff;
  font-size: .95rem;
}
.panel__user-email {
  font-size: .82rem;
  color: #8899aa;
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/header-menu/
git commit -m "feat(frontend): make header-menu auth-aware with login/logout/my-tickets"
```

---

## Task 12: Ticket API service + Book page

**Files:**
- Create: `frontend/src/app/services/ticket-api.service.ts`
- Create: `frontend/src/app/pages/book/book.ts`
- Create: `frontend/src/app/pages/book/book.html`
- Create: `frontend/src/app/pages/book/book.scss`

- [ ] **Step 1: Create ticket-api.service.ts**

```typescript
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface PurchaseRequest {
  tripId: number;
  fromStationId: number;
  toStationId: number;
  price: number;
}

export interface TicketDto {
  id: number;
  userId: number;
  trip: any;
  fromStation: { id: number; name: string; city: string; code: string };
  toStation: { id: number; name: string; city: string; code: string };
  seatNumber: number | null;
  price: number;
  status: string;
  paymentStatus: string;
  ticketCode: string;
  purchasedAt: string;
}

@Injectable({ providedIn: 'root' })
export class TicketApiService {
  private http = inject(HttpClient);

  purchase(req: PurchaseRequest): Observable<TicketDto> {
    return this.http.post<TicketDto>('/api/tickets/purchase', req);
  }

  pay(ticketId: number): Observable<TicketDto> {
    return this.http.post<TicketDto>(`/api/tickets/${ticketId}/pay`, {});
  }

  cancel(ticketId: number): Observable<TicketDto> {
    return this.http.post<TicketDto>(`/api/tickets/${ticketId}/cancel`, {});
  }

  getUserTickets(userId: number): Observable<TicketDto[]> {
    return this.http.get<TicketDto[]>(`/api/tickets/user/${userId}`);
  }
}
```

- [ ] **Step 2: Create book.ts**

```typescript
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { TripResponseDto } from '../../services/trip.service';
import { TicketApiService } from '../../services/ticket-api.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-book',
  imports: [CommonModule],
  templateUrl: './book.html',
  styleUrl: './book.scss'
})
export class BookPage implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private http = inject(HttpClient);
  private ticketApi = inject(TicketApiService);
  private auth = inject(AuthService);

  trip = signal<TripResponseDto | null>(null);
  fromStationId = signal<number>(0);
  toStationId = signal<number>(0);
  loading = signal(false);
  error = signal<string | null>(null);
  success = signal(false);

  ngOnInit() {
    const tripId = Number(this.route.snapshot.paramMap.get('tripId'));
    const from = Number(this.route.snapshot.queryParamMap.get('from'));
    const to = Number(this.route.snapshot.queryParamMap.get('to'));
    this.fromStationId.set(from);
    this.toStationId.set(to);
    this.http.get<TripResponseDto>(`/api/trips/${tripId}`).subscribe({
      next: t => this.trip.set(t),
      error: () => this.error.set('Nie znaleziono połączenia.')
    });
  }

  pay() {
    const t = this.trip();
    if (!t) return;
    this.loading.set(true);
    this.error.set(null);
    const basePrice = 29.90;
    this.ticketApi.purchase({
      tripId: t.id,
      fromStationId: this.fromStationId(),
      toStationId: this.toStationId(),
      price: basePrice
    }).subscribe({
      next: ticket => {
        this.ticketApi.pay(ticket.id).subscribe({
          next: () => { this.success.set(true); this.loading.set(false); },
          error: () => { this.error.set('Błąd płatności.'); this.loading.set(false); }
        });
      },
      error: () => { this.error.set('Nie udało się zarezerwować biletu.'); this.loading.set(false); }
    });
  }

  goMyTickets() { this.router.navigate(['/my-tickets']); }
  goBack() { this.router.navigate(['/']); }
}
```

- [ ] **Step 3: Create book.html**

```html
<div class="book-page">
  <div class="book-card">
    <button type="button" class="book-card__back" (click)="goBack()">
      ← Wróć
    </button>

    <h1 class="book-card__title">Kup bilet</h1>

    @if (success()) {
      <div class="book-success">
        <svg viewBox="0 0 24 24" width="48" height="48" aria-hidden="true">
          <circle cx="12" cy="12" r="10" fill="none" stroke="#3ed3c4" stroke-width="1.8"/>
          <path d="M7 12l3.5 3.5L17 8" stroke="#3ed3c4" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <p>Bilet opłacony!</p>
        <button type="button" class="book-btn" (click)="goMyTickets()">Moje bilety</button>
      </div>
    } @else if (trip()) {
      <div class="book-trip">
        <div class="book-trip__row">
          <span class="book-trip__label">Trasa</span>
          <span class="book-trip__value">{{ trip()!.routeName }}</span>
        </div>
        <div class="book-trip__row">
          <span class="book-trip__label">Odjazd</span>
          <span class="book-trip__value">{{ trip()!.departureTime | slice:0:16 }}</span>
        </div>
        <div class="book-trip__row">
          <span class="book-trip__label">Przyjazd</span>
          <span class="book-trip__value">{{ trip()!.arrivalTime | slice:0:16 }}</span>
        </div>
        <div class="book-trip__row">
          <span class="book-trip__label">Pociąg</span>
          <span class="book-trip__value">{{ trip()!.train.type }} {{ trip()!.train.number }}</span>
        </div>
        <div class="book-trip__row book-trip__row--price">
          <span class="book-trip__label">Cena</span>
          <span class="book-trip__price">29,90 zł</span>
        </div>
      </div>

      @if (error()) {
        <p class="book-error">{{ error() }}</p>
      }

      <button type="button" class="book-btn" [disabled]="loading()" (click)="pay()">
        {{ loading() ? 'Przetwarzanie…' : 'Zapłać 29,90 zł' }}
      </button>
    } @else if (error()) {
      <p class="book-error">{{ error() }}</p>
    } @else {
      <p class="book-loading">Ładowanie…</p>
    }
  </div>
</div>
```

- [ ] **Step 4: Create book.scss**

```scss
.book-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0d1b2a;
}

.book-card {
  background: #162032;
  border: 1px solid rgba(255,255,255,.08);
  border-radius: 16px;
  padding: 40px 36px;
  width: 100%;
  max-width: 460px;
  display: flex;
  flex-direction: column;
  gap: 24px;

  &__back {
    background: none;
    border: none;
    color: #8899aa;
    font-size: .9rem;
    cursor: pointer;
    padding: 0;
    text-align: left;
    &:hover { color: #fff; }
  }

  &__title {
    color: #fff;
    font-size: 1.5rem;
    font-weight: 700;
    margin: 0;
  }
}

.book-trip {
  display: flex;
  flex-direction: column;
  gap: 12px;

  &__row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid rgba(255,255,255,.06);

    &--price {
      border-bottom: none;
      margin-top: 4px;
    }
  }

  &__label { color: #8899aa; font-size: .9rem; }
  &__value { color: #fff; font-size: .95rem; }
  &__price { color: #3ed3c4; font-size: 1.25rem; font-weight: 700; }
}

.book-btn {
  width: 100%;
  padding: 14px;
  background: #3ed3c4;
  color: #0d1b2a;
  font-weight: 700;
  font-size: 1rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  &:disabled { opacity: .5; cursor: not-allowed; }
}

.book-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 16px 0;

  p { color: #fff; font-size: 1.1rem; font-weight: 600; margin: 0; }
}

.book-error {
  color: #ff6b6b;
  font-size: .88rem;
  margin: 0;
}

.book-loading {
  color: #8899aa;
  margin: 0;
}
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/services/ticket-api.service.ts frontend/src/app/pages/book/
git commit -m "feat(frontend): add ticket API service and book page"
```

---

## Task 13: My Tickets page

**Files:**
- Create: `frontend/src/app/pages/my-tickets/my-tickets.ts`
- Create: `frontend/src/app/pages/my-tickets/my-tickets.html`
- Create: `frontend/src/app/pages/my-tickets/my-tickets.scss`

- [ ] **Step 1: Create my-tickets.ts**

```typescript
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TicketApiService, TicketDto } from '../../services/ticket-api.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-my-tickets',
  imports: [CommonModule],
  templateUrl: './my-tickets.html',
  styleUrl: './my-tickets.scss'
})
export class MyTicketsPage implements OnInit {
  private ticketApi = inject(TicketApiService);
  private auth = inject(AuthService);
  private router = inject(Router);

  tickets = signal<TicketDto[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  ngOnInit() {
    const userId = this.auth.currentUser()?.id;
    if (!userId) { this.router.navigate(['/login']); return; }
    this.ticketApi.getUserTickets(userId).subscribe({
      next: t => { this.tickets.set(t); this.loading.set(false); },
      error: () => { this.error.set('Błąd ładowania biletów.'); this.loading.set(false); }
    });
  }

  cancel(ticket: TicketDto) {
    this.ticketApi.cancel(ticket.id).subscribe({
      next: updated => {
        this.tickets.update(list =>
          list.map(t => t.id === updated.id ? updated : t)
        );
      },
      error: () => this.error.set('Nie udało się anulować biletu.')
    });
  }

  goHome() { this.router.navigate(['/']); }
}
```

- [ ] **Step 2: Create my-tickets.html**

```html
<div class="tickets-page">
  <div class="tickets-inner">
    <div class="tickets-header">
      <button type="button" class="tickets-header__back" (click)="goHome()">← Strona główna</button>
      <h1 class="tickets-header__title">Moje bilety</h1>
    </div>

    @if (loading()) {
      <p class="tickets-msg">Ładowanie…</p>
    } @else if (error()) {
      <p class="tickets-msg tickets-msg--error">{{ error() }}</p>
    } @else if (tickets().length === 0) {
      <p class="tickets-msg">Nie masz jeszcze żadnych biletów.</p>
    } @else {
      <ul class="tickets-list" role="list">
        @for (t of tickets(); track t.id) {
          <li class="ticket-card">
            <div class="ticket-card__top">
              <span class="ticket-card__route">{{ t.fromStation.name }} → {{ t.toStation.name }}</span>
              <span class="ticket-card__code">{{ t.ticketCode }}</span>
            </div>
            <div class="ticket-card__mid">
              <div class="ticket-card__detail">
                <span class="ticket-card__label">Odjazd</span>
                <span class="ticket-card__value">{{ t.trip.departureTime | slice:0:16 }}</span>
              </div>
              <div class="ticket-card__detail">
                <span class="ticket-card__label">Cena</span>
                <span class="ticket-card__value">{{ t.price | number:'1.2-2' }} zł</span>
              </div>
              <div class="ticket-card__detail">
                <span class="ticket-card__label">Status</span>
                <span class="ticket-card__badge"
                      [class.ticket-card__badge--active]="t.status === 'ACTIVE' && t.paymentStatus === 'PAID'"
                      [class.ticket-card__badge--pending]="t.paymentStatus === 'PENDING'"
                      [class.ticket-card__badge--cancelled]="t.status === 'CANCELLED'">
                  {{ t.status === 'CANCELLED' ? 'Anulowany' : t.paymentStatus === 'PAID' ? 'Aktywny' : 'Oczekuje' }}
                </span>
              </div>
            </div>
            @if (t.status === 'ACTIVE' && t.paymentStatus === 'PAID') {
              <button type="button" class="ticket-card__cancel" (click)="cancel(t)">
                Anuluj bilet
              </button>
            }
          </li>
        }
      </ul>
    }
  </div>
</div>
```

- [ ] **Step 3: Create my-tickets.scss**

```scss
.tickets-page {
  min-height: 100vh;
  background: #0d1b2a;
  padding: 32px 16px;
}

.tickets-inner {
  max-width: 720px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.tickets-header {
  display: flex;
  flex-direction: column;
  gap: 8px;

  &__back {
    background: none;
    border: none;
    color: #8899aa;
    font-size: .9rem;
    cursor: pointer;
    padding: 0;
    text-align: left;
    width: fit-content;
    &:hover { color: #fff; }
  }

  &__title {
    color: #fff;
    font-size: 1.8rem;
    font-weight: 700;
    margin: 0;
  }
}

.tickets-msg {
  color: #8899aa;
  &--error { color: #ff6b6b; }
}

.tickets-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ticket-card {
  background: #162032;
  border: 1px solid rgba(255,255,255,.08);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;

  &__top {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__route { color: #fff; font-weight: 600; font-size: 1rem; }
  &__code { color: #3ed3c4; font-size: .82rem; font-family: monospace; }

  &__mid {
    display: flex;
    gap: 24px;
    flex-wrap: wrap;
  }

  &__detail {
    display: flex;
    flex-direction: column;
    gap: 3px;
  }

  &__label { color: #8899aa; font-size: .8rem; }
  &__value { color: #fff; font-size: .95rem; }

  &__badge {
    display: inline-block;
    padding: 3px 10px;
    border-radius: 20px;
    font-size: .8rem;
    font-weight: 600;
    background: rgba(255,255,255,.08);
    color: #8899aa;

    &--active { background: rgba(62,211,196,.15); color: #3ed3c4; }
    &--pending { background: rgba(255,200,80,.12); color: #ffc850; }
    &--cancelled { background: rgba(255,107,107,.12); color: #ff6b6b; }
  }

  &__cancel {
    background: none;
    border: 1px solid rgba(255,107,107,.4);
    color: #ff6b6b;
    border-radius: 6px;
    padding: 7px 14px;
    font-size: .88rem;
    cursor: pointer;
    align-self: flex-start;
    &:hover { background: rgba(255,107,107,.08); }
  }
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/pages/my-tickets/
git commit -m "feat(frontend): add my-tickets page with cancel support"
```

---

## Task 14: Wire up routes

**Files:**
- Modify: `frontend/src/app/app.routes.ts`

- [ ] **Step 1: Replace app.routes.ts**

```typescript
import { Routes } from '@angular/router';
import { HomeComponent } from './home/home';
import { LoginPage } from './pages/login/login';
import { RegisterPage } from './pages/register/register';
import { BookPage } from './pages/book/book';
import { MyTicketsPage } from './pages/my-tickets/my-tickets';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginPage },
  { path: 'register', component: RegisterPage },
  { path: 'book/:tripId', component: BookPage, canActivate: [authGuard] },
  { path: 'my-tickets', component: MyTicketsPage, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
```

- [ ] **Step 2: Add `results__buy` styles to home/home.scss** (the Kup button in search results)

Add at the end of `home/home.scss`:

```scss
.results__buy {
  margin-left: auto;
  background: #3ed3c4;
  color: #0d1b2a;
  border: none;
  border-radius: 6px;
  padding: 6px 16px;
  font-weight: 700;
  font-size: .88rem;
  cursor: pointer;
  &:hover { background: #2fbcae; }
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/app.routes.ts frontend/src/app/home/home.scss
git commit -m "feat(frontend): wire up all routes (home, login, register, book, my-tickets)"
```

---

## Task 15: Final backend restart + smoke test

- [ ] **Step 1: Restart backend** (stop existing process, start fresh)

Kill existing Spring Boot process (PID from earlier), then:

```
cd monolith && mvnw.cmd spring-boot:run
```

Expect: Flyway runs V2 migration (adds payment_status column), Spring Boot starts on port 9090.

- [ ] **Step 2: Test login endpoint**

```
curl -s -X POST http://localhost:9090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123"}' 
```

If no user exists yet, register one first:
```
curl -s -X POST http://localhost:9090/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Jan","lastName":"Kowalski","email":"jan@test.com","password":"password123"}'
```

Then login. Expected: `{"token":"eyJ...","user":{...}}`

- [ ] **Step 3: Test actuator**

```
curl http://localhost:9090/actuator/health
```

Expected: `{"status":"UP"}`

- [ ] **Step 4: Restart Angular dev server** and open `http://localhost:4200`

Verify:
- Home page loads with search form
- "Zaloguj się" in header navigates to `/login`
- Login with registered user works, header shows user's first name
- After login, search and click "Kup" navigates to `/book/:id`
- "Zapłać" purchases ticket and redirects to `/my-tickets`
- My Tickets page shows the purchased ticket as "Aktywny"
- Cancel button changes status to "Anulowany"
- Logout clears the header back to "Zaloguj się"

- [ ] **Step 5: Final commit**

```bash
git add .
git commit -m "feat: complete auth, booking flow, my-tickets, k8s health probes, flyway, JSON logging"
```
