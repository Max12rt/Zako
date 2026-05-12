# Spec: Zako — Kubernetes Comparison (Monolith vs Microservices)

**Date:** 2026-05-12  
**Branch monolith:** `main` (after merge of `feat/koleo-homepage`)  
**Branch microservices:** `feat/microservices` (from `main`)  
**Goal:** Deploy both architectures on Kubernetes (Kind), run k6 load tests, collect metrics, write a formal Polish sprawozdanie comparing the two.

---

## 1. Scope

### In scope
- Fix the `/api/tickets/my` endpoint bug in the monolith (k6 test calls it, backend only has `/api/tickets/user/{userId}`)
- Push all uncommitted files from `feat/koleo-homepage` (k6/, k8s/50-52, k8s/60-hpa, kind-cluster.yaml etc.) and merge to `main`
- Create `feat/microservices` branch: split monolith into 3 Spring Boot services with separate Postgres DBs and Kafka event bus
- k8s manifests for microservices in namespace `zako-ms` (runs alongside monolith in `zako`)
- k6 load tests parameterised by `BASE_URL` — same scripts run on both architectures
- Sprawozdanie (`.docx` via pandoc from Markdown source) in Polish

### Out of scope
- CI/CD pipelines
- Production cloud deployment
- Service mesh (Istio/Linkerd)
- Frontend changes for microservices (Ingress routing is transparent)

---

## 2. Monolith fixes (Phase 1)

### 2.1 Add `/api/tickets/my` endpoint

The k6 `load-test.js` calls `GET /api/tickets/my` (authenticated), but `TicketController` only exposes `GET /api/tickets/user/{userId}`. Add a new endpoint:

```
GET /api/tickets/my  →  reads userId from JWT (Authentication principal)
```

This is also the correct secure pattern — the frontend's `my-tickets` page should migrate to this endpoint too (avoids exposing userId in URL).

### 2.2 Push & merge

Commit and push all locally-present-but-untracked files:
- `k6/smoke-test.js`, `k6/load-test.js`
- `k8s/15-app-secret.yaml`, `k8s/50-prometheus.yaml`, `k8s/51-grafana.yaml`, `k8s/52-jaeger.yaml`, `k8s/60-hpa.yaml`
- `kind-cluster.yaml`, `run-monolith.ps1`, `setup-kind.ps1`, `setup-minikube.ps1`
- `monolith/.mvn/jvm.config`

Merge `feat/koleo-homepage` → `main`.

---

## 3. Microservices architecture (Phase 2)

### 3.1 Service decomposition

| Service | Responsibility | Port | DB |
|---|---|---|---|
| `auth-service` | User registration, login, JWT issuance | 8081 | `auth-db` |
| `trip-service` | Stations, trains, routes, trips, seat availability | 8082 | `trip-db` |
| `ticket-service` | Purchase, pay, cancel tickets | 8083 | `ticket-db` |

**JWT strategy:** shared secret (`JWT_SECRET` env var) in all services. Each service validates the token independently — no service-to-service auth calls.

**ticket-service → trip-service dependency:** ticket-service stores `trip_id`, `from_station_id`, `to_station_id` as plain longs (no FK validation against trip-service at purchase time — client is trusted via JWT). trip-service reacts to Kafka events to maintain seat counts.

### 3.2 Directory structure

```
microservices/
  auth-service/
    src/main/java/zako/auth/
    src/main/resources/application.properties
    Dockerfile
    pom.xml
  trip-service/
    src/main/java/zako/trip/
    src/main/resources/application.properties
    Dockerfile
    pom.xml
  ticket-service/
    src/main/java/zako/ticket/
    src/main/resources/application.properties
    Dockerfile
    pom.xml
  pom.xml  (parent, packaging=pom)
k8s-microservices/
  00-namespace.yaml          (namespace: zako-ms)
  10-kafka.yaml              (bitnami Kafka single-broker, KRaft mode)
  20-auth-db.yaml            (Postgres StatefulSet + Service)
  21-auth-service.yaml       (Deployment + Service + ConfigMap)
  30-trip-db.yaml
  31-trip-service.yaml
  40-ticket-db.yaml
  41-ticket-service.yaml
  50-ingress.yaml            (host: zako-ms.localhost)
  60-hpa.yaml                (HPA for all 3 services)
  70-prometheus.yaml         (scrapes all 3 /actuator/prometheus)
  71-grafana.yaml
```

### 3.3 Kafka topics and events

| Topic | Producer | Consumer | Payload |
|---|---|---|---|
| `ticket.purchased` | ticket-service | trip-service | `{tripId, fromStationId, toStationId, seats: 1}` |
| `ticket.cancelled` | ticket-service | trip-service | `{tripId, seats: 1}` |
| `ticket.paid` | ticket-service | — (informational) | `{ticketId, tripId}` |

trip-service decrements/increments `trips.available_seats` on consume.

### 3.4 Databases

Each service has its own Postgres StatefulSet (`postgres:16-alpine`) with Flyway managing its schema. Schemas are strictly isolated — no cross-service table access.

| Service | DB name | Tables |
|---|---|---|
| auth-service | `auth_db` | users |
| trip-service | `trip_db` | stations, trains, routes, route_stops, trips |
| ticket-service | `ticket_db` | tickets |

Demo data seeded via Flyway migrations:
- `auth-db`: no demo users — k6 `setup()` registers the test user at runtime
- `trip-db`: V2 migration seeds stations, trains, routes, trips (same data as monolith `DemoDataSeeder`)
- `ticket-db`: no seed data

### 3.5 API surface (unchanged from monolith)

Ingress routes `zako-ms.localhost` → services:

```
/api/auth/**      → auth-service:8081
/api/users/**     → auth-service:8081
/api/stations/**  → trip-service:8082
/api/trains/**    → trip-service:8082
/api/trips/**     → trip-service:8082
/api/tickets/**   → ticket-service:8083
/actuator/**      → (not exposed externally)
/grafana          → grafana:3000
/jaeger           → jaeger:16686
/                 → frontend:80
```

Frontend serves from the same image — no changes needed.

### 3.6 Observability

Each service exposes `/actuator/health` and `/actuator/prometheus`. Prometheus in `zako-ms` scrapes all three. Grafana shows per-service CPU/memory/latency panels. Jaeger collects OTLP traces from all services.

---

## 4. Kubernetes cluster setup

Both architectures run in the same Kind cluster (`kind-cluster.yaml` already present):
- Namespace `zako` — monolith
- Namespace `zako-ms` — microservices

Hosts file entries required:
```
127.0.0.1  zako.localhost
127.0.0.1  zako-ms.localhost
```

Ingress controller: `ingress-nginx` (already in `setup-kind.ps1`).

HPA config per architecture:

| Target | Min | Max | CPU trigger |
|---|---|---|---|
| monolith | 1 | 5 | 60% |
| auth-service | 1 | 3 | 60% |
| trip-service | 1 | 5 | 60% |
| ticket-service | 1 | 5 | 60% |

---

## 5. Load testing

### Scripts (in `k6/`)

| Script | Purpose |
|---|---|
| `smoke-test.js` | 1 VU, 30s — sanity check both archs |
| `load-test.js` | Staged ramp-up to 100 VUs — main comparison |

Both use `BASE_URL` env var: `http://zako.localhost` or `http://zako-ms.localhost`.

### Test scenario (`load-test.js`)
1. Anonymous: `GET /api/trips` (search)
2. Auth: `POST /api/auth/login`
3. Authenticated: `GET /api/tickets/my`
4. (Optional) Purchase: `POST /api/tickets/purchase`

### Metrics collected

| Metric | Tool |
|---|---|
| p50/p95/p99 latency | k6 |
| Error rate | k6 |
| HPA replica count over time | `kubectl get hpa -w` |
| CPU/memory per pod | Grafana (Prometheus) |
| Trace latency breakdown | Jaeger |

Run sequence:
1. Deploy monolith → warm up (smoke) → load test → export k6 summary JSON + Grafana screenshots
2. Deploy microservices → warm up → load test → export results
3. Compare in sprawozdanie

---

## 6. Sprawozdanie

**Output:** `docs/sprawozdanie.md` (Markdown source) → `docs/sprawozdanie.docx` (via `pandoc sprawozdanie.md -o sprawozdanie.docx`)

**Language:** Polish

**Structure:**

1. **Wstęp** — cel laboratorium, opis systemu Zako (zakup biletów kolejowych), zakres porównania
2. **Opis architektury monolitycznej** — diagram, komponenty, jeden deployment, jedna baza
3. **Opis architektury mikroserwisowej** — diagram, 3 serwisy, Kafka, osobne bazy, mapowanie API
4. **Środowisko testowe** — Kind cluster, specyfikacja węzła, wersje narzędzi (k8s, Spring Boot, k6)
5. **Metodologia testów** — opis scenariusza k6, stages (ramp-up, steady, spike), metryki
6. **Wyniki — monolith** — tabela k6 (p95, p99, error rate), wykres HPA, CPU/memory
7. **Wyniki — mikroserwisy** — j.w.
8. **Porównanie** — tabela zbiorcza, analiza różnic (skalowanie, latencja, fault isolation)
9. **Wnioski** — kiedy używać monolit, kiedy mikroserwisy, wpływ Kafki na latencję

**Placeholders for real metrics** (to be filled after running tests):
- `[METRIC_MONOLITH_P95]`, `[METRIC_MS_P95]` etc.
- Grafana screenshots: `docs/img/grafana-monolith.png`, `docs/img/grafana-ms.png`

---

## 7. Implementation order

1. Fix monolith (`/api/tickets/my`), commit, push, merge to `main`
2. Create `feat/microservices` from `main`
3. Scaffold `auth-service` (copy auth + user domain from monolith)
4. Scaffold `trip-service` (copy station + train + route + trip domain)
5. Scaffold `ticket-service` (copy ticket domain, add Kafka producer)
6. Add Kafka consumer to `trip-service`
7. Write k8s manifests for `zako-ms` namespace
8. Build Docker images, load into Kind
9. Deploy and smoke-test microservices
10. Run k6 load tests on both, collect metrics
11. Write sprawozdanie.md with real metrics, generate .docx
