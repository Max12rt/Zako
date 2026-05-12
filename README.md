# Zako

KOLEO-style train ticket booking demo. Monolithic Spring Boot backend, Angular frontend, PostgreSQL.

```
zako/
├── frontend/          Angular 21 + SCSS (KOLEO-styled UI)
├── monolith/          Spring Boot 4 / Java 21 / JPA / Spring Security
├── k8s/               Kubernetes manifests (postgres + monolith + frontend + ingress)
├── docker-compose.yml Local stack: postgres + monolith + frontend
└── pom.xml            Maven aggregator
```

## Stack

- **Frontend:** Angular 21 (standalone components, signals), pure SCSS, HttpClient
- **Backend:** Java 21, Spring Boot 4.0, Spring Web MVC, Spring Data JPA, Spring Security, Lombok
- **Database:** PostgreSQL 16
- **Container:** Multi-stage Dockerfiles (Maven→JRE for backend, Node→nginx for frontend)
- **Orchestration:** Kubernetes (StatefulSet for postgres, Deployments for app tier, Ingress)

## Run locally — three options

### 1. Native dev mode

Postgres locally on `:5432` with database `ticket_monolith_db`, user `postgres`, password `1234`.

```bash
# backend
cd monolith && ./mvnw spring-boot:run

# frontend (in another terminal)
cd frontend && npm install && npm start
```

Open http://localhost:4200/. Frontend dev server proxies `/api/*` to `:9090` (see `frontend/src/proxy.conf.json`).

### 2. Docker Compose

```bash
docker compose up --build
```

- Frontend: http://localhost:8080/
- API:      http://localhost:9090/api/stations
- Postgres: `localhost:5432`

### 3. Kubernetes

See [k8s/README.md](k8s/README.md).

```bash
docker build -t zako/monolith:latest ./monolith
docker build -t zako/frontend:latest ./frontend
kubectl apply -f k8s/
```

## Features

- KOLEO-style homepage (navy hero, search form, operators band)
- Drop-down menu (`Zaloguj się`, `Załóż konto`, account sections, language)
- Custom calendar + time picker with Polish locale and month navigation
- Station autocomplete (debounced, keyboard-navigable)
- Geolocation: "Najbl." button on departure field calls `/api/stations/nearest`
- Trip search wired to `POST /api/trips/search`
- 20 Polish stations seeded on first boot

## Endpoints

- `GET  /api/stations` — list all
- `GET  /api/stations/search?query=` — autocomplete
- `GET  /api/stations/nearest?lat=&lon=` — closest station
- `POST /api/trips/search` — search by from/to/date
- (full CRUD for users, trains, routes, tickets in respective controllers)
