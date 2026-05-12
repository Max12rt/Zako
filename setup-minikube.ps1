# Full minikube setup for ZAKO observability stack
# Run from C:\Users\crulp\zako:  .\setup-minikube.ps1

$ErrorActionPreference = 'Stop'

# Ensure ~/bin (where minikube lives) is in PATH for this session
$env:PATH += ";$env:USERPROFILE\bin"

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }

# 1. Start minikube
Step "Starting minikube"
minikube start --cpus=4 --memory=6g --driver=docker

# 2. Enable required addons
Step "Enabling addons: ingress, metrics-server"
minikube addons enable ingress
minikube addons enable metrics-server

# 3. Point Docker to minikube daemon so images are available inside the cluster
Step "Configuring Docker to build inside minikube"
& minikube docker-env --shell powershell | Invoke-Expression

# 4. Build images
Step "Building monolith image"
docker build -t zako/monolith:latest ./monolith

Step "Building frontend image"
docker build -t zako/frontend:latest ./frontend

# 5. Apply manifests in order
Step "Applying K8s manifests"
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/10-postgres-secret.yaml
kubectl apply -f k8s/15-app-secret.yaml
kubectl apply -f k8s/11-postgres-statefulset.yaml
kubectl apply -f k8s/20-monolith-config.yaml
kubectl apply -f k8s/21-monolith.yaml
kubectl apply -f k8s/30-frontend.yaml
kubectl apply -f k8s/50-prometheus.yaml
kubectl apply -f k8s/51-grafana.yaml
kubectl apply -f k8s/52-jaeger.yaml
kubectl apply -f k8s/60-hpa.yaml
kubectl apply -f k8s/40-ingress.yaml

# 6. Wait for monolith to be ready
Step "Waiting for monolith to be ready (up to 3 min)"
kubectl rollout status deployment/monolith -n zako --timeout=180s

# 7. Add hosts entry if missing
Step "Checking /etc/hosts for zako.localhost"
$hostsFile = "$env:SystemRoot\System32\drivers\etc\hosts"
$minikubeIp = minikube ip
if (-not (Select-String -Path $hostsFile -Pattern "zako.localhost" -Quiet)) {
    Write-Host "  Adding $minikubeIp zako.localhost to hosts (requires admin)" -ForegroundColor Yellow
    Add-Content -Path $hostsFile -Value "`n$minikubeIp zako.localhost" -Encoding ASCII
} else {
    Write-Host "  zako.localhost already in hosts" -ForegroundColor Green
}

# 8. Print summary
$ip = minikube ip
Step "Done! Access points:"
Write-Host "  App:        http://zako.localhost"           -ForegroundColor Green
Write-Host "  API:        http://zako.localhost/api/trips" -ForegroundColor Green
Write-Host "  Grafana:    http://zako.localhost/grafana    (admin/admin)" -ForegroundColor Green
Write-Host "  Jaeger:     http://zako.localhost/jaeger"    -ForegroundColor Green
Write-Host "  Prometheus: http://zako.localhost/actuator/prometheus" -ForegroundColor Green
Write-Host ""
Write-Host "  k6 smoke:   k6 run k6/smoke-test.js" -ForegroundColor DarkGray
Write-Host "  k6 load:    k6 run k6/load-test.js"  -ForegroundColor DarkGray
Write-Host "  HPA watch:  kubectl get hpa -n zako -w" -ForegroundColor DarkGray
