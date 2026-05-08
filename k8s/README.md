# Kubernetes — Zako

Manifests apply in lexicographic order: namespace → postgres secret → postgres → monolith config/deployment → frontend → ingress.

## Build images locally (e.g. into Minikube / Docker Desktop)

```bash
docker build -t zako/monolith:latest ./monolith
docker build -t zako/frontend:latest ./frontend
```

For Minikube, run `eval $(minikube docker-env)` before building so images land inside the cluster.
For kind: `kind load docker-image zako/monolith:latest && kind load docker-image zako/frontend:latest`.

## Apply

```bash
kubectl apply -f k8s/
```

## Verify

```bash
kubectl -n zako get pods,svc,pvc
kubectl -n zako logs deploy/monolith
```

## Access

With NGINX Ingress controller installed and `zako.localhost` resolving to the cluster:
- Frontend: http://zako.localhost/
- API: http://zako.localhost/api/stations

For port-forward without Ingress:

```bash
kubectl -n zako port-forward svc/frontend 8080:80
kubectl -n zako port-forward svc/monolith 9090:9090
```

## Secrets

`postgres-secret` ships with the same dev password used in `application.properties` and `docker-compose.yml`. Replace before any non-local deploy.
