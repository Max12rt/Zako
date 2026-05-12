# Kubernetes — Zako Microservices (`zako-ms`)

Parallel deployment of the auth, trip, and ticket microservices. Each service owns its Postgres StatefulSet; Kafka (KRaft mode, single broker) carries seat-update events from ticket-service to trip-service.

Manifests apply in lexicographic order: namespace → postgres-{auth,trip,ticket} → app secret → Kafka → service ConfigMaps/Deployments/Services → frontend → ingress → HPA.

## Build images locally

```bash
docker build -t zako/auth-service:latest   -f microservices/auth-service/Dockerfile   microservices
docker build -t zako/trip-service:latest   -f microservices/trip-service/Dockerfile   microservices
docker build -t zako/ticket-service:latest -f microservices/ticket-service/Dockerfile microservices
docker build -t zako/frontend:latest ./frontend
```

For Minikube: `eval $(minikube docker-env)` first. For kind: `kind load docker-image zako/<service>:latest`.

## Apply

```bash
kubectl apply -f k8s/microservices/
```

## Verify

```bash
kubectl -n zako-ms get pods,svc,pvc,hpa
kubectl -n zako-ms logs deploy/auth-service
kubectl -n zako-ms logs deploy/trip-service
kubectl -n zako-ms logs deploy/ticket-service
```

## Access

With NGINX Ingress and `zako-ms.localhost` resolving to the cluster:
- Frontend: http://zako-ms.localhost/
- Auth API: http://zako-ms.localhost/api/auth/login
- Trips API: http://zako-ms.localhost/api/trips
- Tickets API: http://zako-ms.localhost/api/tickets/my

Port-forward fallback:

```bash
kubectl -n zako-ms port-forward svc/auth-service   8081:8081
kubectl -n zako-ms port-forward svc/trip-service   8082:8082
kubectl -n zako-ms port-forward svc/ticket-service 8083:8083
```

## Topology

```
       ┌──────────┐
       │ frontend │
       └────┬─────┘
            │ /api/auth/*      → auth-service   :8081 → postgres-auth
            │ /api/users/*     → auth-service
ingress ───►│ /api/stations,/api/trips,/api/trains → trip-service :8082 → postgres-trip
            │ /api/tickets/*   → ticket-service  :8083 → postgres-ticket
            │                                          ↘ Kafka (ticket.purchased / cancelled)
            │                                            ↘ trip-service consumer → seat decrement
```

## Secrets

`postgres-{auth,trip,ticket}-secret` and `app-secret` ship with dev credentials matching `application.properties` / `docker-compose.yml`. Replace before any non-local deploy.
