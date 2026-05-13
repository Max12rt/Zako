# Zako — Architektura porównawcza: Monolit vs Mikroserwisy

Dokument zawiera komplet diagramów obu wariantów wdrożenia aplikacji **Zako** (rezerwacja biletów kolejowych): wersji monolitycznej (Spring Boot na pojedynczej bazie Postgres) oraz wersji mikroserwisowej (auth + trip + ticket + Kafka).

---

## 1. Kontekst systemu (System Context)

Wszystkie komponenty zewnętrzne (użytkownik, baza, broker) i to, jak rozmawiają z aplikacją.

```mermaid
flowchart LR
    subgraph User["Użytkownik końcowy"]
        Browser["Przeglądarka<br/>(Angular SPA)"]
    end

    subgraph Mono["Monolit (zako)"]
        FE1["nginx + frontend<br/>:8080"]
        BE1["Spring Boot<br/>:9090"]
        DB1["PostgreSQL<br/>ticket_monolith_db"]
        FE1 -->|reverse-proxy /api/| BE1
        BE1 -->|JDBC| DB1
    end

    subgraph MS["Mikroserwisy (zako-ms)"]
        FE2["nginx + frontend<br/>:8090"]
        AUTH["auth-service<br/>:8081"]
        TRIP["trip-service<br/>:8082"]
        TICK["ticket-service<br/>:8083"]
        KAFKA[("Apache Kafka<br/>KRaft :9092")]
        DBA[("auth_db<br/>:5433")]
        DBT[("trip_db<br/>:5434")]
        DBK[("ticket_db<br/>:5435")]

        FE2 -->|/api/auth, /api/users| AUTH
        FE2 -->|/api/stations, /api/trips, /api/trains| TRIP
        FE2 -->|/api/tickets| TICK
        AUTH -->|JDBC| DBA
        TRIP -->|JDBC| DBT
        TICK -->|JDBC| DBK
        TICK -->|REST GET /api/trips,/api/stations| TRIP
        TICK -.->|publish ticket.purchased / cancelled| KAFKA
        KAFKA -.->|consume| TRIP
    end

    Browser ===> FE1
    Browser ===> FE2
```

---

## 2. Monolit — diagram kontenerów

Pojedynczy proces JVM obsługujący wszystkie domeny biznesowe. Jeden schemat bazy.

```mermaid
flowchart TB
    subgraph Compose["docker-compose / k8s namespace: zako"]
        FE["frontend<br/>nginx :8080<br/>(Angular dist + reverse proxy /api/)"]
        BE["monolith<br/>Spring Boot :9090<br/>WebMVC + Security + JPA"]
        DB[("PostgreSQL :5432<br/>ticket_monolith_db")]
        FE -- "HTTP /api/*" --> BE
        BE -- "JDBC" --> DB
    end
    Browser["Przeglądarka"] -->|HTTPS / HTTP :8080| FE
```

### 2.1 Pakiety / komponenty monolitu

```mermaid
flowchart TB
    subgraph App["zako.monolith"]
        direction TB
        Auth["auth/<br/>AuthController · AuthService<br/>JwtService · JwtAuthFilter"]
        Users["user/<br/>UserController · UserService<br/>UserRepository · User"]
        Tickets["ticket/<br/>TicketController · TicketService<br/>TicketRepository · Ticket"]
        Trips["trip/<br/>TripController · TripService<br/>TripRepository · Trip"]
        Stations["station/<br/>StationController · StationService<br/>Station · Route · RouteStop"]
        Trains["train/<br/>TrainController · TrainService<br/>Train"]
        Config["config/<br/>SecurityConfig · CorsConfig"]
        Seeder["seeder/<br/>StationSeeder · DemoDataSeeder"]
        Obs["observability/<br/>metrics, tracing, logging"]

        Auth --> Users
        Tickets --> Trips
        Tickets --> Users
        Tickets --> Stations
        Trips --> Trains
        Trips --> Stations
        Stations --> Stations
    end
```

### 2.2 Model danych monolitu — ER

Jedna baza, klucze obce między wszystkimi encjami.

```mermaid
erDiagram
    USERS ||--o{ TICKETS       : "buys"
    TRIPS ||--o{ TICKETS       : "for"
    TRAINS ||--o{ TRIPS        : "operates"
    ROUTES ||--o{ TRIPS        : "follows"
    ROUTES ||--o{ ROUTE_STOPS  : "has"
    STATIONS ||--o{ ROUTE_STOPS : "is_stop"
    STATIONS ||--o{ TICKETS_FROM : "departs_from"
    STATIONS ||--o{ TICKETS_TO   : "arrives_at"

    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar first_name
        varchar last_name
        varchar role
        timestamp created_at
    }
    STATIONS {
        bigint id PK
        varchar name UK
        varchar city
        varchar code UK
        double latitude
        double longitude
    }
    TRAINS {
        bigint id PK
        varchar train_number UK
        varchar type
        int total_seats
    }
    ROUTES {
        bigint id PK
        varchar name
    }
    ROUTE_STOPS {
        bigint id PK
        bigint route_id FK
        bigint station_id FK
        int stop_order
        int minutes_from_start
    }
    TRIPS {
        bigint id PK
        bigint train_id FK
        bigint route_id FK
        timestamp departure_time
        timestamp arrival_time
        varchar status
        int available_seats
    }
    TICKETS {
        bigint id PK
        bigint user_id FK
        bigint trip_id FK
        bigint from_station_id FK
        bigint to_station_id FK
        int seat_number
        numeric price
        varchar status
        varchar payment_status
        varchar ticket_code UK
        timestamp purchased_at
    }
```

---

## 3. Mikroserwisy — diagram kontenerów

Trzy niezależne procesy JVM, każdy z własną bazą; broker Kafka łączy producenta zdarzeń biletowych z konsumentem.

```mermaid
flowchart LR
    Browser["Przeglądarka"] --> FE
    FE["frontend<br/>nginx :8090"]:::ms

    subgraph Auth_Stack["auth domain"]
        AUTH["auth-service<br/>:8081"]:::ms
        DBA[("postgres-auth<br/>auth_db :5433")]
        AUTH --> DBA
    end

    subgraph Trip_Stack["trip / catalog domain"]
        TRIP["trip-service<br/>:8082<br/>Kafka consumer"]:::ms
        DBT[("postgres-trip<br/>trip_db :5434")]
        TRIP --> DBT
    end

    subgraph Ticket_Stack["ticket domain"]
        TICK["ticket-service<br/>:8083<br/>Kafka producer"]:::ms
        DBK[("postgres-ticket<br/>ticket_db :5435")]
        TICK --> DBK
    end

    KAFKA[("Apache Kafka<br/>KRaft :9092<br/>topics: ticket.purchased, ticket.cancelled")]

    FE -->|/api/auth,/api/users| AUTH
    FE -->|/api/stations,/api/trips,/api/trains| TRIP
    FE -->|/api/tickets| TICK

    TICK -->|REST sync: trip&station lookup| TRIP
    TICK -->|publish event| KAFKA
    KAFKA -->|consume event<br/>decrement / restore seats| TRIP

    classDef ms fill:#1b3a4b,stroke:#3ed3c4,color:#fff
```

### 3.1 Pakiety / komponenty mikroserwisów

```mermaid
flowchart TB
    subgraph AUTH["auth-service · zako.auth"]
        AuthC["auth/<br/>AuthController · AuthService"]
        UserC["user/<br/>UserController · UserService<br/>User · UserRepository"]
        AuthCfg["config/<br/>SecurityConfig · JwtService · JwtAuthFilter"]
        AuthC --> UserC
    end

    subgraph TRIP["trip-service · zako.trip"]
        StationC["station/<br/>StationController · StationService<br/>Station · Repository"]
        TrainC["train/<br/>TrainController · TrainService<br/>Train · Repository"]
        TripC["trip/<br/>TripController · TripService<br/>Trip · Repository"]
        RouteC["route/<br/>Route · RouteStop · Repository"]
        KafkaC["kafka/<br/>TicketEventConsumer · TicketEvent"]
        TripCfg["config/<br/>SecurityConfig · JwtService · JwtAuthFilter"]
        SeedT["seeder/<br/>StationSeeder · DemoDataSeeder"]
        TripC --> TrainC
        TripC --> RouteC
        RouteC --> StationC
        KafkaC --> TripC
    end

    subgraph TICK["ticket-service · zako.ticket"]
        TickC["ticket/<br/>TicketController · TicketService<br/>Ticket · Repository"]
        Client["client/<br/>TripServiceClient (RestClient)"]
        KafkaP["kafka/<br/>TicketEventProducer · KafkaProducerConfig"]
        TickCfg["config/<br/>SecurityConfig · JwtService · JwtAuthFilter"]
        TickC --> Client
        TickC --> KafkaP
    end

    Client -.->|HTTP| TRIP
    KafkaP -.->|publish| KafkaC
```

### 3.2 Model danych mikroserwisów — trzy ER, brak FK między bazami

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar first_name
        varchar last_name
        varchar role
        timestamp created_at
    }
```
*auth_db — tylko tabela `users`.*

```mermaid
erDiagram
    TRAINS ||--o{ TRIPS        : "operates"
    ROUTES ||--o{ TRIPS        : "follows"
    ROUTES ||--o{ ROUTE_STOPS  : "has"
    STATIONS ||--o{ ROUTE_STOPS : "is_stop"

    STATIONS {
        bigint id PK
        varchar name UK
        varchar city
        varchar code UK
        double latitude
        double longitude
    }
    TRAINS {
        bigint id PK
        varchar train_number UK
        varchar type
        int total_seats
    }
    ROUTES {
        bigint id PK
        varchar name
    }
    ROUTE_STOPS {
        bigint id PK
        bigint route_id FK
        bigint station_id FK
        int stop_order
        int minutes_from_start
    }
    TRIPS {
        bigint id PK
        bigint train_id FK
        bigint route_id FK
        timestamp departure_time
        timestamp arrival_time
        varchar status
        int available_seats
    }
```
*trip_db — `stations` / `trains` / `routes` / `route_stops` / `trips`.*

```mermaid
erDiagram
    TICKETS {
        bigint id PK
        bigint user_id "no FK — z auth_db"
        bigint trip_id "no FK — z trip_db"
        bigint from_station_id "no FK — z trip_db"
        varchar from_station_name "denormalised"
        varchar from_station_city "denormalised"
        varchar from_station_code "denormalised"
        bigint to_station_id "no FK — z trip_db"
        varchar to_station_name "denormalised"
        varchar to_station_city "denormalised"
        varchar to_station_code "denormalised"
        int seat_number
        timestamp departure_time "denormalised — Flyway V2"
        timestamp arrival_time "denormalised — Flyway V2"
        numeric price
        varchar status
        varchar payment_status
        varchar ticket_code UK
        timestamp purchased_at
    }
```
*ticket_db — pojedyncza `tickets`; obce identyfikatory są wyłącznie liczbami, dane z innych domen denormalizowane w momencie zakupu.*

---

## 4. Sekwencja — rejestracja użytkownika

### 4.1 Monolit

```mermaid
sequenceDiagram
    autonumber
    participant U as Użytkownik
    participant N as nginx :8080
    participant M as Monolith :9090
    participant DB as ticket_monolith_db

    U->>N: POST /api/users/register {email,password,name}
    N->>M: proxy_pass /api/users/register
    M->>M: validate, BCrypt password
    M->>DB: INSERT INTO users
    DB-->>M: id
    M-->>N: 201 UserResponse{id,email,role,createdAt}
    N-->>U: 201
```

### 4.2 Mikroserwisy

```mermaid
sequenceDiagram
    autonumber
    participant U as Użytkownik
    participant N as nginx :8090
    participant A as auth-service :8081
    participant DBA as auth_db

    U->>N: POST /api/users/register
    N->>A: proxy_pass /api/users/register
    A->>A: validate, BCrypt password
    A->>DBA: INSERT INTO users
    DBA-->>A: id
    A-->>N: 201 UserResponse
    N-->>U: 201
```
*Identyczne na pierwszy rzut oka — różnica jest w warstwie infrastruktury: w monolicie ten sam proces obsłuży później kupno biletu; tutaj auth-service nigdy nie zobaczy zapytania biletowego.*

---

## 5. Sekwencja — zakup biletu

### 5.1 Monolit (jedna transakcja)

```mermaid
sequenceDiagram
    autonumber
    participant U as Użytkownik
    participant M as Monolith :9090
    participant DB as ticket_monolith_db

    U->>M: POST /api/tickets/purchase<br/>Bearer JWT
    M->>M: JwtAuthFilter → load User
    Note over M,DB: BEGIN
    M->>DB: SELECT trip WHERE id = :tripId
    M->>DB: SELECT from_station / to_station
    M->>M: check trip.availableSeats > 0
    M->>DB: UPDATE trips SET available_seats = available_seats - 1
    M->>DB: INSERT INTO tickets (...)
    Note over M,DB: COMMIT
    M-->>U: 201 TicketResponse
```

### 5.2 Mikroserwisy (REST + Kafka, eventual consistency)

```mermaid
sequenceDiagram
    autonumber
    participant U as Użytkownik
    participant T as ticket-service :8083
    participant TS as trip-service :8082
    participant DBK as ticket_db
    participant K as Kafka
    participant DBT as trip_db

    U->>T: POST /api/tickets/purchase {tripId,fromStationId,toStationId,price}<br/>Bearer JWT
    T->>T: JwtAuthFilter → userId z claim
    T->>TS: GET /api/trips/{tripId}
    TS-->>T: TripDto{id, availableSeats, departureTime, arrivalTime}
    T->>T: check availableSeats > 0
    T->>TS: GET /api/stations/{from} & /{to}
    TS-->>T: StationDto × 2
    T->>DBK: INSERT INTO tickets (with denormalised station + time fields)
    DBK-->>T: ticket.id
    T->>K: publish ticket.purchased {ticketId, tripId, seats:1}
    T-->>U: 201 TicketResponse
    Note over K,DBT: asynchronicznie
    K->>TS: consume ticket.purchased
    TS->>DBT: UPDATE trips SET available_seats = available_seats - 1
```

### 5.3 Sekwencja — anulowanie biletu (mikroserwisy)

```mermaid
sequenceDiagram
    autonumber
    participant U as Użytkownik
    participant T as ticket-service
    participant DBK as ticket_db
    participant K as Kafka
    participant TS as trip-service
    participant DBT as trip_db

    U->>T: POST /api/tickets/{id}/cancel
    T->>DBK: SELECT ticket WHERE id=:id
    T->>T: assert owner = userId && status = ACTIVE
    T->>DBK: UPDATE tickets SET status='CANCELLED'
    T->>K: publish ticket.cancelled
    T-->>U: 200 TicketResponse
    K->>TS: consume ticket.cancelled
    TS->>DBT: UPDATE trips SET available_seats = available_seats + 1
```

---

## 6. Wdrożenie — docker-compose

### 6.1 Monolit (`docker-compose.yml`)

```mermaid
flowchart LR
    subgraph Net["sieć zako_default"]
        FE["zako-frontend<br/>:8080 → :80"]
        BE["zako-monolith<br/>:9090"]
        PG["zako-postgres<br/>:5432<br/>volume postgres-data"]
        FE -- proxy_pass /api/ --> BE
        BE -- JDBC --> PG
    end
```

### 6.2 Mikroserwisy (`microservices/docker-compose.yml`)

```mermaid
flowchart TB
    subgraph Net["sieć microservices_default"]
        FE["zako-ms-frontend<br/>:8090 → :80"]
        AU["zako-ms-auth<br/>:8081"]
        TR["zako-ms-trip<br/>:8082"]
        TI["zako-ms-ticket<br/>:8083"]
        K["zako-ms-kafka<br/>:9092"]
        PA["zako-ms-postgres-auth<br/>:5433 → :5432"]
        PT["zako-ms-postgres-trip<br/>:5434 → :5432"]
        PK["zako-ms-postgres-ticket<br/>:5435 → :5432"]

        FE --> AU
        FE --> TR
        FE --> TI
        AU --> PA
        TR --> PT
        TI --> PK
        TI -.REST.-> TR
        TI -.produce.-> K
        K -.consume.-> TR
    end
```

---

## 7. Wdrożenie — Kubernetes

### 7.1 Namespace `zako` (monolit)

```mermaid
flowchart TB
    subgraph NS["namespace: zako"]
        ING["Ingress zako<br/>host: zako.localhost"]
        SVC_FE["Service frontend :80"]
        SVC_BE["Service monolith :9090"]
        SVC_PG["Service postgres :5432 (headless)"]
        DEP_FE["Deployment frontend<br/>replicas: 2"]
        DEP_BE["Deployment monolith<br/>HPA 1–5 (CPU 60%, MEM 75%)"]
        STS_PG["StatefulSet postgres<br/>PVC 2Gi"]
        CM["ConfigMap monolith-config"]
        SEC1["Secret postgres-secret"]
        SEC2["Secret app-secret (JWT)"]

        ING --> SVC_FE
        ING --> SVC_BE
        SVC_FE --> DEP_FE
        SVC_BE --> DEP_BE
        SVC_PG --> STS_PG
        DEP_BE --> SVC_PG
        DEP_BE -. envFrom .-> CM
        DEP_BE -. env .-> SEC1
        DEP_BE -. env .-> SEC2
        STS_PG -. envFrom .-> SEC1
    end
```

### 7.2 Namespace `zako-ms` (mikroserwisy)

```mermaid
flowchart TB
    subgraph NS["namespace: zako-ms"]
        ING["Ingress zako-ms<br/>host: zako-ms.localhost"]
        FE_S["Service frontend"]
        AU_S["Service auth-service :8081"]
        TR_S["Service trip-service :8082"]
        TI_S["Service ticket-service :8083"]
        K_S["Service kafka :9092"]
        PA_S["Service postgres-auth (headless)"]
        PT_S["Service postgres-trip (headless)"]
        PK_S["Service postgres-ticket (headless)"]

        FE_D["Deployment frontend ×2"]
        AU_D["Deployment auth-service<br/>HPA 1–3"]
        TR_D["Deployment trip-service<br/>HPA 1–5"]
        TI_D["Deployment ticket-service<br/>HPA 1–5"]
        K_STS["StatefulSet kafka<br/>PVC 2Gi (KRaft)"]
        PA_STS["StatefulSet postgres-auth<br/>PVC 1Gi"]
        PT_STS["StatefulSet postgres-trip<br/>PVC 1Gi"]
        PK_STS["StatefulSet postgres-ticket<br/>PVC 1Gi"]

        ING --> FE_S
        ING --> AU_S
        ING --> TR_S
        ING --> TI_S

        FE_S --> FE_D
        AU_S --> AU_D
        TR_S --> TR_D
        TI_S --> TI_D
        K_S  --> K_STS
        PA_S --> PA_STS
        PT_S --> PT_STS
        PK_S --> PK_STS

        AU_D --> PA_S
        TR_D --> PT_S
        TI_D --> PK_S
        TI_D --> TR_S
        TI_D --> K_S
        TR_D --> K_S
    end
```

---

## 8. Porównanie — monolit vs mikroserwisy

| Aspekt | Monolit | Mikroserwisy |
|---|---|---|
| Procesy JVM | 1 | 3 (auth + trip + ticket) |
| Bazy Postgres | 1 (ticket_monolith_db) | 3 (auth_db, trip_db, ticket_db) |
| Schemat danych | Wspólny, FK między tabelami | Rozdzielony, brak FK między bazami |
| Komunikacja modułów | wywołania w obrębie JVM (`@Service`) | REST sync (`ticket → trip`) + Kafka async (`ticket → trip`) |
| Transakcyjność | Lokalna `@Transactional` obejmuje wszystkie domeny | Lokalna w obrębie serwisu; spójność seatów: *eventual consistency* przez Kafkę |
| Autoryzacja | Ten sam `SecurityFilterChain` waliduje JWT dla wszystkich `/api/*` | Każdy serwis ma własny `JwtAuthFilter` z tym samym sekretem |
| Wdrożenie (k8s) | 1 Deployment + 1 StatefulSet | 3 Deployment + 4 StatefulSet (3 × Postgres + Kafka) |
| Skalowanie | HPA jednego Deploymentu — wszystko skaluje się razem | HPA na każdy serwis — `trip-service` z 5 replikami, `auth-service` z 3 |
| Awaria | Awaria procesu = brak całej funkcjonalności | Awaria `ticket-service` nie blokuje przeglądania / logowania |
| Czas startu | ~14 s | 3 × ~25–30 s (auth + trip + ticket) + Kafka |

---

## 9. Wybrane decyzje projektowe

1. **Denormalizacja stacji + czasów w `tickets`** (mikroserwisy). Bilet po sprzedaży nie zależy od późniejszych zmian w `trip_db` — frontend wyświetla nazwy i daty bez zapytań N+1 do `trip-service`.
2. **Identyfikatory bez FK** między bazami. Ticket trzyma `user_id`, `trip_id`, `from_station_id` jako liczby, weryfikacja istnienia odbywa się raz, przy zakupie, przez REST do `trip-service`.
3. **Kafka KRaft single-node** zamiast klastra z ZooKeeper — wystarczające dla demo, te same listenery wewnątrz sieci kontenera (`kafka:9092`) i na zewnątrz (`localhost:29092`).
4. **CORS** — monolit używał `setAllowedOrigins("http://localhost:4200")` z czasów dev-servera Angulara; zmienione na `setAllowedOriginPatterns("http://localhost:*", "http://zako.localhost", "http://zako-ms.localhost")`, by frontend ze stacku Docker / k8s mógł rozmawiać z backendem.
5. **`spring-boot-flyway` starter** w `ticket-service` — `flyway-core` sam z siebie nie aktywuje autoconfig w Spring Boot 4.0, więc migracja V1 była po cichu pomijana i tabela `tickets` nie powstawała. Dodanie startera naprawia oba.
