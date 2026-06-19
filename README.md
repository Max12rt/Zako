# Zako

KOLEO-style train ticket booking demo. Implemented in two architectures for performance comparison: a **monolithic** Spring Boot backend and an equivalent **microservices** stack (auth / trip / ticket / notification) with Kafka.

```
zako/
├── frontend/              Angular 21 + SCSS (KOLEO-styled UI)
├── monolith/               Spring Boot 4 / Java 21 / JPA / Spring Security — monolith variant
├── microservices/          auth-service, trip-service, ticket-service, notification-service
├── k8s/                    Kubernetes manifests
│   ├── (monolith manifests, root level)
│   └── microservices/      Manifests for zako-ms namespace (services, Kafka, 3x Postgres)
├── k6/                     Load + security test scripts (k6)
├── results/                Raw output of every test run (.txt)
├── docs/                   Architecture notes, specs
├── docker-compose.yml      Local stack: postgres + monolith + frontend
└── pom.xml                 Maven aggregator
```

## Stack

- **Frontend:** Angular 21 (standalone components, signals), pure SCSS, HttpClient
- **Backend:** Java 21, Spring Boot 4.0, Spring Web MVC, Spring Data JPA, Spring Security, Lombok
- **Database:** PostgreSQL 16 (1 instance for monolith, 3 isolated instances for microservices)
- **Messaging:** Apache Kafka (microservices only — ticket events → notification-service)
- **Realtime:** WebSocket / STOMP over SockJS (notification-service)
- **Container:** Multi-stage Dockerfiles (Maven→JRE for backend, Node→nginx for frontend)
- **Orchestration:** Kubernetes (StatefulSets for Postgres/Kafka, Deployments for app tier, Ingress, HPA)
- **Monitoring:** Prometheus + Grafana (kube-prometheus-stack)
- **Load/Security testing:** k6 (Grafana k6)

## Features

- KOLEO-style homepage (navy hero, search form, operators band)
- Drop-down menu (`Zaloguj się`, `Załóż konto`, account sections, language)
- Custom calendar + time picker with Polish locale and month navigation
- Station autocomplete (debounced, keyboard-navigable)
- Geolocation: "Najbl." button on departure field calls `/api/stations/nearest`
- Trip search wired to `POST /api/trips/search`
- Real-time ticket-purchase notifications via WebSocket (microservices variant only)
- 20 Polish stations seeded on first boot

## Endpoints

- `GET  /api/stations` — list all
- `GET  /api/stations/search?query=` — autocomplete
- `GET  /api/stations/nearest?lat=&lon=` — closest station
- `POST /api/trips/search` — search by from/to/date
- `POST /api/auth/login`, `POST /api/users/register` — auth
- `GET  /api/tickets/my` — current user's tickets
- (full CRUD for users, trains, routes, tickets in respective controllers)

---

# Quick start — three ways to run it

## 1. Native dev mode

Postgres locally on `:5432`, database `ticket_monolith_db`, user `postgres`, password `1234`.

```bash
cd monolith && ./mvnw spring-boot:run

# in another terminal
cd frontend && npm install && npm start
```

Open http://localhost:4200/. Frontend dev server proxies `/api/*` to `:9090`.

## 2. Docker Compose

### Monolith
```bash
docker compose up --build
```
Frontend: http://localhost:8080/ · API: http://localhost:9090/api/stations · Postgres: `localhost:5432`

### Microservices
```bash
cd microservices
docker compose up --build
```
Frontend: http://localhost:8090/ · auth: `:8081` · trip: `:8082` · ticket: `:8083` · notification: `:8084` · Kafka: `:9092`

## 3. Kubernetes (used for all performance/security testing below)

### Prerequisites
- Docker Desktop with Kubernetes enabled (tested with 2 CPU / 5 GB RAM limit)
- `kubectl`, `k6` installed locally (`brew install k6`)

### Build images

```bash
# Monolith
docker build -t monolith:latest -f monolith/Dockerfile .
docker build -t frontend:latest -f frontend/Dockerfile ./frontend

# Microservices — build from inside microservices/ (Dockerfiles expect this context)
cd microservices
docker build -t auth-service:latest -f auth-service/Dockerfile .
docker build -t trip-service:latest -f trip-service/Dockerfile .
docker build -t ticket-service:latest -f ticket-service/Dockerfile .
docker build -t notification-service:latest -f notification-service/Dockerfile .
cd ..
```

### Deploy

```bash
# Monolith → namespace zako
kubectl apply -f k8s/ -n zako

# Microservices → namespace zako-ms (includes auth/trip/ticket/notification, Kafka, 3x Postgres)
kubectl apply -f k8s/microservices/ -n zako-ms

# Verify everything is Running
kubectl get pods -n zako
kubectl get pods -n zako-ms
```

If any microservice pod crash-loops with `UnknownHostException: postgres-*`, the per-service Postgres StatefulSets haven't come up yet — wait ~30s and `kubectl rollout restart deployment <name> -n zako-ms`.

### Port-forward for local access

```bash
# Monolith
kubectl port-forward svc/monolith 9090:9090 -n zako &

# Microservices — frontend (nginx, routes /api/* to the right service)
kubectl port-forward svc/frontend 8090:80 -n zako-ms &

# Microservices — direct service access (used by the security/perf test scripts below,
# since nginx redirects /api/trips → /api/trips/ which breaks k6's BASE_URL handling)
kubectl port-forward svc/auth-service 8081:8081 -n zako-ms &
kubectl port-forward svc/trip-service 8082:8082 -n zako-ms &
kubectl port-forward svc/ticket-service 8083:8083 -n zako-ms &
kubectl port-forward svc/notification-service 8084:8084 -n zako-ms &
```

### Grafana

```bash
kubectl port-forward svc/grafana 3000:3000 -n zako &
# or, if using kube-prometheus-stack in `monitoring` namespace:
kubectl port-forward svc/prometheus-grafana 3000:80 -n monitoring &
```
Open http://localhost:3000 — default login `admin` / `admin` (or `prom-operator` for kube-prometheus-stack). Get the generated password with:
```bash
kubectl get secret -n monitoring prometheus-grafana -o jsonpath="{.data.admin-password}" | base64 -d
```
Dashboards to check: **Kubernetes / Compute Resources / Namespace** (CPU/Memory per pod) and **Kubernetes / Compute Resources / Cluster** (overall).

---

# Running the test suite

All scripts live in `k6/`. Raw results of every run already done are archived in `results/`.

## 1. Smoke test (sanity check, 1 VU / 30s)

```bash
mkdir -p results
k6 run -e BASE_URL=http://localhost:9090 k6/smoke-test.js | tee results/monolith-smoke.txt
k6 run -e BASE_URL=http://localhost:8090 k6/smoke-test.js | tee results/microservices-smoke.txt
```

## 2. Load test (100 VU spike, 10 min, monolith vs microservices)

```bash
# Monolith
k6 run -e BASE_URL=http://localhost:9090 k6/load-test.js | tee results/monolith-load.txt

# Microservices (direct ports — bypasses nginx redirect issue)
k6 run k6/load-test-ms.js | tee results/microservices-load.txt

# Notification service (WebSocket/health under load)
k6 run k6/notification-test.js | tee results/notification-load.txt
```

Thresholds checked: `http_req_failed<5%`, `http_req_duration p(95)<1000ms / p(99)<2000ms`, `trip_search_duration p(95)<800ms`, `auth_duration p(95)<600ms`.

## 3. Security tests

### DDoS flood (200 req/s constant arrival rate, 2 min)
```bash
k6 run -e BASE_URL=http://localhost:9090 k6/ddos-test.js | tee results/monolith-ddos.txt
k6 run -e BASE_URL=http://localhost:8082 k6/ddos-test.js | tee results/microservices-ddos.txt
```

### Brute force (30 VU dictionary attack on /api/auth/login, 2 min)
```bash
k6 run -e BASE_URL=http://localhost:9090 k6/bruteforce-test.js | tee results/monolith-bruteforce.txt
k6 run -e BASE_URL=http://localhost:8081 k6/bruteforce-test.js | tee results/microservices-bruteforce.txt
```

### JWT manipulation (20 VU, forged/empty/alg:none tokens, 1 min)
```bash
k6 run -e BASE_URL=http://localhost:9090 k6/jwt-test.js | tee results/monolith-jwt.txt
k6 run -e BASE_URL=http://localhost:8081 k6/jwt-test.js | tee results/microservices-jwt.txt
```

> Keep each `kubectl port-forward` running in its own terminal/background job while a test executes — k6 will report `connection refused` if the tunnel drops mid-test (this happened repeatedly during testing on a laptop; the monolith pod itself also crash-restarted under the 100 VU load test — see results below).

---

# Results summary

Full analysis with all tables and conclusions: **[`docs/raport_mikroserwisy_vs_monolit_v4.md`](docs/raport_mikroserwisy_vs_monolit_v4.md)**.

| Test | Monolith | Microservices |
|---|---|---|
| Load p(95) @ 100VU | 1940 ms — **FAIL**, pod killed by k8s (SIGTERM, liveness timeout) | 95 ms — **PASS** |
| Throughput | 43.5 req/s | 84.9 req/s (+95%) |
| DDoS (200 req/s, 2 min) | 0% HTTP errors, p(95) = **2.84 s** (heavy degradation) | 0% HTTP errors, p(95) = **377 ms** |
| Brute force (auth) | 100% blocked (401), no rate-limit | **0% blocked** — needs investigation, see report |
| JWT manipulation | 100% rejected, p(95) = 23 ms | 100% rejected, p(95) = 23 ms |

Raw k6 output for every run is in `results/*.txt`.

---

# Architecture notes

- `monolith` — single Spring Boot process, one Postgres instance, port `9090`.
- `microservices` — 4 independent Spring Boot services + Kafka + 3 isolated Postgres instances:
  - `auth-service` (`:8081`) — JWT issuing/validation
  - `trip-service` (`:8082`) — routes & schedules
  - `ticket-service` (`:8083`) — ticket purchase, publishes Kafka events
  - `notification-service` (`:8084`) — consumes Kafka ticket events, pushes via WebSocket/STOMP
- See `docs/superpowers/specs/2026-06-16-notification-service-design.md` for the notification service design doc.
